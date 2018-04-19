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

package bisq.core.dao.consensus.state.events.payloads;

import bisq.core.dao.consensus.vote.proposal.ProposalType;
import bisq.core.dao.consensus.vote.proposal.param.Param;

import bisq.network.p2p.NodeAddress;

import bisq.common.app.Version;
import bisq.common.crypto.Sig;

import io.bisq.generated.protobuffer.PB;

import java.security.PublicKey;

import java.util.Date;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

//TODO separate value object with p2p network data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public final class RemoveAssetProposalPayload extends Proposal {

    public RemoveAssetProposalPayload(String uid,
                                      String name,
                                      String title,
                                      String description,
                                      String link,
                                      NodeAddress nodeAddress,
                                      PublicKey ownerPubKey,
                                      Date creationDate) {
        super(uid,
                name,
                title,
                description,
                link,
                Sig.getPublicKeyBytes(ownerPubKey),
                Version.PROPOSAL,
                creationDate.getTime(),
                null,
                null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private RemoveAssetProposalPayload(String uid,
                                       String name,
                                       String title,
                                       String description,
                                       String link,
                                       String nodeAddress,
                                       byte[] ownerPubKeyEncoded,
                                       byte version,
                                       long creationDate,
                                       String txId,
                                       @Nullable Map<String, String> extraDataMap) {
        super(uid,
                name,
                title,
                description,
                link,
                ownerPubKeyEncoded,
                version,
                creationDate,
                txId,
                extraDataMap);
    }

    @Override
    public PB.Proposal.Builder getProposalBuilder() {
        //TODO impl
        return null;
    }

    public static RemoveAssetProposalPayload fromProto(PB.Proposal proto) {
        //TODO impl
        return null;
    }

    @Override
    public Proposal cloneWithoutTxId() {
        //TODO impl
        return null;
    }

    @Override
    public Proposal cloneWithTxId(String txId) {
        //TODO impl
        return null;
    }

    @Override
    public ProposalType getType() {
        return ProposalType.REMOVE_ALTCOIN;
    }

    @Override
    public Param getQuorumDaoParam() {
        return Param.QUORUM_REMOVE_ASSET;
    }

    @Override
    public Param getThresholdDaoParam() {
        return Param.THRESHOLD_REMOVE_ASSET;
    }
}