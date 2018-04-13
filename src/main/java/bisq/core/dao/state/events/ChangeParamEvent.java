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

package bisq.core.dao.state.events;

import bisq.core.dao.vote.proposal.param.ChangeParamPayload;
import bisq.core.dao.vote.proposal.param.DaoParam;

import io.bisq.generated.protobuffer.PB;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ChangeParamEvent extends StateChangeEvent {

    public ChangeParamEvent(ChangeParamPayload changeParamPayload, int blockHeight) {
        super(changeParamPayload, blockHeight);
    }

    public long getValue() {
        return getChangeParam().getValue();
    }

    public DaoParam getDaoParam() {
        return (getChangeParam()).getDaoParam();
    }

    private ChangeParamPayload getChangeParam() {
        return (ChangeParamPayload) getPayload();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public PB.ChangeParamEvent toProtoMessage() {
        final PB.ChangeParamEvent.Builder builder = PB.ChangeParamEvent.newBuilder()
                .setChangeParamPayload(getChangeParam().toProtoMessage())
                .setHeight(getHeight());
        return builder.build();
    }

    public static ChangeParamEvent fromProto(PB.ChangeParamEvent proto) {
        return new ChangeParamEvent(ChangeParamPayload.fromProto(proto.getChangeParamPayload()),
                proto.getHeight());
    }
}