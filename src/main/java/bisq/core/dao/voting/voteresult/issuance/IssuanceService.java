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

package bisq.core.dao.voting.voteresult.issuance;

import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.Tx;
import bisq.core.dao.state.blockchain.TxInput;
import bisq.core.dao.state.blockchain.TxOutput;
import bisq.core.dao.state.ext.Issuance;
import bisq.core.dao.state.period.DaoPhase;
import bisq.core.dao.state.period.PeriodService;
import bisq.core.dao.voting.proposal.compensation.CompensationProposal;

import javax.inject.Inject;

import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

//TODO case that user misses reveal phase not impl. yet

@Slf4j
public class IssuanceService {
    private final StateService stateService;
    private final PeriodService periodService;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public IssuanceService(StateService stateService, PeriodService periodService) {
        this.stateService = stateService;
        this.periodService = periodService;
    }

    public void issueBsq(CompensationProposal compensationProposal, int chainHeight) {
        final Set<TxOutput> compReqIssuanceTxOutputs = stateService.getIssuanceCandidateTxOutputs();
        compReqIssuanceTxOutputs.stream()
                .filter(txOutput -> isValid(txOutput, compensationProposal, periodService, chainHeight))
                .forEach(txOutput -> {
                    long amount = compensationProposal.getRequestedBsq().value;
                    long date = compensationProposal.getCreationDate().getTime();
                    Optional<Tx> optionalTx = stateService.getTx(compensationProposal.getTxId());
                    if (optionalTx.isPresent()) {
                        Tx tx = optionalTx.get();
                        // We use key from first input
                        TxInput txInput = tx.getInputs().get(0);
                        String inputPubKey = txInput.getPubKey();
                        Issuance issuance = new Issuance(txOutput, chainHeight, amount, inputPubKey, date);
                        stateService.addIssuance(issuance);

                        StringBuilder sb = new StringBuilder();
                        sb.append("\n################################################################################\n");
                        sb.append("We issued new BSQ to tx with ID ").append(txOutput.getTxId())
                                .append("\nfor compensationProposal with UID ").append(compensationProposal.getUid())
                                .append("\n################################################################################\n");
                        log.info(sb.toString());
                    } else {
                        log.error("Tx for compensation request not found. txId={}", compensationProposal.getTxId());
                    }
                });
    }

    private boolean isValid(TxOutput txOutput, CompensationProposal compensationProposal, PeriodService periodService, int chainHeight) {
        return txOutput.getTxId().equals(compensationProposal.getTxId())
                && compensationProposal.getRequestedBsq().value == txOutput.getValue()
                && compensationProposal.getBsqAddress().substring(1).equals(txOutput.getAddress())
                && periodService.isTxInCorrectCycle(txOutput.getTxId(), chainHeight)
                && periodService.isTxInPhase(txOutput.getTxId(), DaoPhase.Phase.PROPOSAL);
    }
}