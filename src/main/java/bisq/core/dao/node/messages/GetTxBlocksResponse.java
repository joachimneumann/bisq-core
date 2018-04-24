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

package bisq.core.dao.node.messages;

import bisq.core.dao.state.blockchain.TxBlock;

import bisq.network.p2p.DirectMessage;
import bisq.network.p2p.ExtendedDataSizePermission;

import bisq.common.app.Version;
import bisq.common.proto.network.NetworkEnvelope;

import io.bisq.generated.protobuffer.PB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public final class GetTxBlocksResponse extends NetworkEnvelope implements DirectMessage, ExtendedDataSizePermission {
    private final List<TxBlock> txBlocks;
    private final int requestNonce;

    public GetTxBlocksResponse(List<TxBlock> txBlocks, int requestNonce) {
        this(txBlocks, requestNonce, Version.getP2PMessageVersion());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private GetTxBlocksResponse(List<TxBlock> txBlocks, int requestNonce, int messageVersion) {
        super(messageVersion);
        this.txBlocks = txBlocks;
        this.requestNonce = requestNonce;
    }

    @Override
    public PB.NetworkEnvelope toProtoNetworkEnvelope() {
        return getNetworkEnvelopeBuilder()
                .setGetTxBlocksResponse(PB.GetTxBlocksResponse.newBuilder()
                        .addAllTxBlocks(txBlocks.stream()
                                .map(TxBlock::toProtoMessage)
                                .collect(Collectors.toList()))
                        .setRequestNonce(requestNonce))
                .build();
    }

    public static NetworkEnvelope fromProto(PB.GetTxBlocksResponse proto, int messageVersion) {
        return new GetTxBlocksResponse(proto.getTxBlocksList().isEmpty() ?
                new ArrayList<>() :
                proto.getTxBlocksList().stream()
                        .map(TxBlock::fromProto)
                        .collect(Collectors.toList()),
                proto.getRequestNonce(),
                messageVersion);
    }
}