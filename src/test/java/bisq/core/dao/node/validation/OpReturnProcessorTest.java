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

import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.OpReturnType;

import bisq.common.app.Version;
import bisq.common.util.Tuple2;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(PowerMockRunner.class)
@PrepareForTest({OpReturnProposalValidator.class, OpReturnCompReqValidator.class,
        OpReturnBlindVoteValidator.class, OpReturnVoteRevealValidator.class, StateService.class})
public class OpReturnProcessorTest {

    private OpReturnProcessor opReturnProcessor;

    @Before
    public void setup() {
        opReturnProcessor = new OpReturnProcessor(
                new OpReturnValuePaddingValidator(),
                mock(OpReturnProposalValidator.class),
                mock(OpReturnCompReqValidator.class),
                mock(OpReturnBlindVoteValidator.class),
                mock(OpReturnVoteRevealValidator.class),
                mock(StateService.class));
    }

    @Test
    public void testGetOpReturnDataForSinglePadding() throws IOException {
        int index = 0;
        int padding = 0;
        byte[] opReturnData = opReturnProcessor.getOpReturnDataForSinglePadding(index, padding);

        assertEquals(OpReturnType.VALUE_PADDING.getType(), opReturnData[0]);
        assertEquals(Version.VALUE_PADDING_VERSION, opReturnData[1]);
        assertEquals(index, opReturnData[2]);

        assertEquals(padding, opReturnProcessor.getPaddingFromOpReturn(opReturnData, index));

        padding = 1;
        opReturnData = opReturnProcessor.getOpReturnDataForSinglePadding(index, padding);
        assertEquals(padding, opReturnProcessor.getPaddingFromOpReturn(opReturnData, index));

        padding = 256;
        opReturnData = opReturnProcessor.getOpReturnDataForSinglePadding(index, padding);
        assertEquals(padding, opReturnProcessor.getPaddingFromOpReturn(opReturnData, index));

        padding = 257;
        opReturnData = opReturnProcessor.getOpReturnDataForSinglePadding(index, padding);
        assertEquals(padding, opReturnProcessor.getPaddingFromOpReturn(opReturnData, index));

        padding = 65535;
        opReturnData = opReturnProcessor.getOpReturnDataForSinglePadding(index, padding);
        assertEquals(padding, opReturnProcessor.getPaddingFromOpReturn(opReturnData, index));

        index = 2;
        padding = 555;
        opReturnData = opReturnProcessor.getOpReturnDataForSinglePadding(index, padding);
        assertEquals(padding, opReturnProcessor.getPaddingFromOpReturn(opReturnData, index));
    }

    @Test
    public void testGetOpReturnDataForPadding() throws IOException {
        List<Tuple2<Integer, Integer>> paddingList = new ArrayList<>();
        paddingList.add(new Tuple2<>(0, 12));
        paddingList.add(new Tuple2<>(1, 33));
        paddingList.add(new Tuple2<>(3, 555));

        byte[] opReturnData = opReturnProcessor.getOpReturnDataForPadding(paddingList);

        assertEquals(OpReturnType.VALUE_PADDING.getType(), opReturnData[0]);
        assertEquals(Version.VALUE_PADDING_VERSION, opReturnData[1]);
        assertEquals(0, opReturnData[2]);
        assertEquals(1, opReturnData[5]);
        assertEquals(3, opReturnData[8]);

        assertEquals(12, opReturnProcessor.getPaddingFromOpReturn(opReturnData, 0));
        assertEquals(33, opReturnProcessor.getPaddingFromOpReturn(opReturnData, 1));
        assertEquals(555, opReturnProcessor.getPaddingFromOpReturn(opReturnData, 3));
    }

}
