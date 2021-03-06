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

package bisq.core.dao.blockchain.vo;

import bisq.common.proto.ProtoUtil;

import io.bisq.generated.protobuffer.PB;

import lombok.Getter;

public enum TxType {
    UNDEFINED_TX_TYPE(false, false),
    UNVERIFIED(false, false),
    INVALID(false, false),
    GENESIS(false, false),
    TRANSFER_BSQ(false, false),
    PAY_TRADE_FEE(false, true),
    PROPOSAL(true, true),
    COMPENSATION_REQUEST(true, true),
    BLIND_VOTE(true, true),
    VOTE_REVEAL(true, false),
    LOCK_UP(true, false),
    UN_LOCK(true, false);


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Getter
    private final boolean hasOpReturn;
    @Getter
    private final boolean requiresFee;

    TxType(boolean hasOpReturn, boolean requiresFee) {
        this.hasOpReturn = hasOpReturn;
        this.requiresFee = requiresFee;
    }

    public static TxType fromProto(PB.TxType txType) {
        return ProtoUtil.enumFromProto(TxType.class, txType.name());
    }

    public PB.TxType toProtoMessage() {
        return PB.TxType.valueOf(name());
    }
}
