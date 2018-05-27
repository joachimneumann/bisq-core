/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao.node.validation;

import bisq.core.dao.state.period.DaoPhase;
import bisq.core.dao.state.period.PeriodService;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies if OP_RETURN data matches rules for a vote reveal tx and applies state change.
 */
@Slf4j
public class OpReturnVoteRevealValidator {
    private final PeriodService periodService;


    @Inject
    public OpReturnVoteRevealValidator(PeriodService periodService) {
        this.periodService = periodService;
    }

    // opReturnData: 2 bytes version and type, 20 bytes hash, 16 bytes key

    // We do not check the version as if we upgrade the a new version old clients would fail. Rather we need to make
    // a change backward compatible so that new clients can handle both versions and old clients are tolerant.
    boolean validate(byte[] opReturnData, int blockHeight, TxState txState) {
        return txState.getInputFromBlindVoteStakeOutput() != null &&
                txState.isSingleInputFromBlindVoteStakeOutput() &&
                txState.getVoteRevealUnlockStakeOutput() != null &&
                opReturnData.length == 38 &&
                periodService.isInPhase(blockHeight, DaoPhase.Phase.VOTE_REVEAL);
    }
}