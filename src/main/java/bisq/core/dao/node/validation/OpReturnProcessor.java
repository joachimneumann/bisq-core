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
import bisq.core.dao.state.blockchain.Tx;
import bisq.core.dao.state.blockchain.TxOutput;
import bisq.core.dao.state.blockchain.TxOutputType;

import bisq.common.app.DevEnv;
import bisq.common.app.Version;
import bisq.common.util.Tuple2;

import org.bitcoinj.core.Utils;

import javax.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

/**
 * Processes OpReturn output if valid and delegates validation to specific validators.
 */
@Slf4j
public class OpReturnProcessor {
    private final OpReturnValuePaddingValidator opReturnValuePaddingValidator;
    private final OpReturnProposalValidator opReturnProposalValidator;
    private final OpReturnCompReqValidator opReturnCompReqValidator;
    private final OpReturnBlindVoteValidator opReturnBlindVoteValidator;
    private final OpReturnVoteRevealValidator opReturnVoteRevealValidator;
    private final StateService stateService;

    @Inject
    public OpReturnProcessor(OpReturnValuePaddingValidator opReturnValuePaddingValidator,
                             OpReturnProposalValidator opReturnProposalValidator,
                             OpReturnCompReqValidator opReturnCompReqValidator,
                             OpReturnBlindVoteValidator opReturnBlindVoteValidator,
                             OpReturnVoteRevealValidator opReturnVoteRevealValidator,
                             StateService stateService) {
        this.opReturnValuePaddingValidator = opReturnValuePaddingValidator;

        this.opReturnProposalValidator = opReturnProposalValidator;
        this.opReturnCompReqValidator = opReturnCompReqValidator;
        this.opReturnBlindVoteValidator = opReturnBlindVoteValidator;
        this.opReturnVoteRevealValidator = opReturnVoteRevealValidator;
        this.stateService = stateService;
    }

    // We only check partially the rules here as we do not know the BSQ fee at that moment which is always used when
    // we have OP_RETURN data.
    public void processOpReturnCandidate(TxOutput txOutput, TxState txState) {
        // We do not check for pubKeyScript.scriptType.NULL_DATA because that is only set if dumpBlockchainData is true
        final byte[] opReturnData = txOutput.getOpReturnData();
        // Padding is not used in opReturn and value has to be 0 anyway, so we ignore handling padding here
        if (txOutput.getValue() == 0 && opReturnData != null && opReturnData.length >= 1) {
            OpReturnType.getOpReturnType(opReturnData[0])
                    .ifPresent(opReturnType -> {
                        // Value padding need to be know before parsing the other outputs
                        if (opReturnType == OpReturnType.VALUE_PADDING) {
                            if (opReturnValuePaddingValidator.validate(opReturnData)) {
                                txState.setOpReturnCandidateData(opReturnData);
                                txState.setOpReturnTypeCandidate(opReturnType);
                            }
                        } else {
                            txState.setOpReturnTypeCandidate(opReturnType);
                        }
                    });
        }
    }

    public void validate(byte[] opReturnData, TxOutput txOutput, Tx tx, int index, long bsqFee,
                         int blockHeight, TxState txState) {
        // Padding is not used in opReturn and value has to be 0 anyway, so we ignore handling padding here
        if (txOutput.getValue() == 0 && index == tx.getOutputs().size() - 1 && opReturnData.length >= 1) {
            final Optional<OpReturnType> optionalOpReturnType = OpReturnType.getOpReturnType(opReturnData[0]);
            if (optionalOpReturnType.isPresent()) {
                selectValidator(opReturnData, txOutput, tx, bsqFee, blockHeight, txState, optionalOpReturnType.get());
            } else {
                log.warn("OP_RETURN data does not match our defined types. opReturnData={}",
                        tx, Utils.HEX.encode(opReturnData));
            }
        } else {
            log.warn("OP_RETURN data does not match our rules. opReturnData={}",
                    tx, Utils.HEX.encode(opReturnData));
        }
    }

    private void selectValidator(byte[] opReturnData, TxOutput txOutput, Tx tx, long bsqFee, int blockHeight,
                                 TxState txState, OpReturnType opReturnType) {
        switch (opReturnType) {
            case VALUE_PADDING:
                processValuePadding(opReturnData, txOutput, txState);
                break;
            case PROPOSAL:
                processProposal(opReturnData, txOutput, bsqFee, blockHeight, txState);
                break;
            case COMPENSATION_REQUEST:
                processCompensationRequest(opReturnData, txOutput, bsqFee, blockHeight, txState);
                break;
            case BLIND_VOTE:
                processBlindVote(opReturnData, txOutput, bsqFee, blockHeight, txState);
                break;
            case VOTE_REVEAL:
                processVoteReveal(opReturnData, txOutput, blockHeight, txState);
                break;
            case LOCK_UP:
                // TODO
                stateService.setTxOutputType(txOutput, TxOutputType.BOND_LOCK_OP_RETURN_OUTPUT);
                break;
            case UNLOCK:
                // TODO
                stateService.setTxOutputType(txOutput, TxOutputType.BOND_UNLOCK_OP_RETURN_OUTPUT);
                break;
            default:
                // Should never happen as long we keep OpReturnType entries in sync with out switch case.
                final String msg = "Unsupported OpReturnType. tx=" + tx +
                        "; opReturnData=" + Utils.HEX.encode(opReturnData);
                log.error(msg);
                if (DevEnv.isDevMode())
                    throw new RuntimeException(msg);

                break;
        }
    }

    private void processValuePadding(byte[] opReturnData, TxOutput txOutput, TxState txState) {
        if (opReturnValuePaddingValidator.validate(opReturnData)) {
            stateService.setTxOutputType(txOutput, TxOutputType.VALUE_PADDING_OP_RETURN_OUTPUT);
            txState.setVerifiedOpReturnType(OpReturnType.VALUE_PADDING);
        } else {
            log.info("We expected a value padding op_return data but it did not " +
                    "match our rules. txOutput={};", txOutput);
            stateService.setTxOutputType(txOutput, TxOutputType.INVALID_OUTPUT);
        }
    }

    public int getPaddingFromOpReturn(@Nullable byte[] opReturnData, int outputIndex) {
        if (opReturnData != null && opReturnValuePaddingValidator.validate(opReturnData)) {
            // First 2 bytes are type and version, after that we get groups of 3 bytes for
            // index (1 byte) and padding (2 bytes)
            for (int i = 2; i < opReturnData.length; i++) {
                // We use first byte for output index. 256 is sufficient to cover possible output indexes.
                int index = (int) opReturnData[i];
                if (index == outputIndex) {
                    // We convert 2 bytes to an integer. 65536 is sufficient for covering the dust (546)
                    byte lowByte = opReturnData[++i];
                    byte highByte = opReturnData[++i];
                    return ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
                } else {
                    i += 2;
                }
            }
        }
        return 0;
    }

    public byte[] getOpReturnDataForPadding(List<Tuple2<Integer, Integer>> paddingList) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(OpReturnType.VALUE_PADDING.getType());
            outputStream.write(Version.VALUE_PADDING_VERSION);

            paddingList.forEach(tuple -> {
                int outputIndex = tuple.first;
                int padding = tuple.second;

                if (padding > 65535) {
                    throw new IllegalArgumentException("padding must not be larger than 65535 (2 bytes)");
                } else if (padding < 0) {
                    throw new IllegalArgumentException("padding must not be negative");
                }

                outputStream.write((byte) outputIndex);
                byte lowByte = (byte) (padding & 0xFF);
                outputStream.write(lowByte);
                byte highByte = (byte) ((padding >>> 8) & 0xFF);
                outputStream.write(highByte);
            });

            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.toString());
            throw e;
        }
    }

    public byte[] getOpReturnDataForSinglePadding(int outputIndex, int padding) throws IOException {
        List<Tuple2<Integer, Integer>> paddingList = new ArrayList<>();
        paddingList.add(new Tuple2<>(outputIndex, padding));
        return getOpReturnDataForPadding(paddingList);
    }

    private void processProposal(byte[] opReturnData, TxOutput txOutput, long bsqFee, int blockHeight, TxState txState) {
        if (opReturnProposalValidator.validate(opReturnData, txOutput, bsqFee, blockHeight, txState)) {
            stateService.setTxOutputType(txOutput, TxOutputType.PROPOSAL_OP_RETURN_OUTPUT);
            txState.setVerifiedOpReturnType(OpReturnType.PROPOSAL);
        } else {
            log.info("We expected a proposal op_return data but it did not " +
                    "match our rules. txOutput={}; blockHeight={}", txOutput, blockHeight);
            stateService.setTxOutputType(txOutput, TxOutputType.INVALID_OUTPUT);
        }
    }

    private void processCompensationRequest(byte[] opReturnData, TxOutput txOutput, long bsqFee, int blockHeight, TxState txState) {
        final TxOutput issuanceCandidate = txState.getIssuanceCandidate();
        if (opReturnCompReqValidator.validate(opReturnData, txOutput, bsqFee, blockHeight, txState)) {
            stateService.setTxOutputType(txOutput, TxOutputType.COMP_REQ_OP_RETURN_OUTPUT);
            stateService.setTxOutputType(issuanceCandidate, TxOutputType.ISSUANCE_CANDIDATE_OUTPUT);
            txState.setVerifiedOpReturnType(OpReturnType.COMPENSATION_REQUEST);
        } else {
            log.info("We expected a compensation request op_return data but it did not " +
                    "match our rules. txOutput={}; blockHeight={}", txOutput, blockHeight);
            stateService.setTxOutputType(txOutput, TxOutputType.INVALID_OUTPUT);

            // If the opReturn is invalid the issuance candidate cannot become BSQ, so we set it to BTC
            if (issuanceCandidate != null)
                stateService.setTxOutputType(issuanceCandidate, TxOutputType.BTC_OUTPUT);
        }
    }

    private void processBlindVote(byte[] opReturnData, TxOutput txOutput, long bsqFee, int blockHeight, TxState txState) {
        final TxOutput blindVoteLockStakeOutput = txState.getBlindVoteLockStakeOutput();
        if (opReturnBlindVoteValidator.validate(opReturnData, bsqFee, blockHeight, txState)) {
            stateService.setTxOutputType(txOutput, TxOutputType.BLIND_VOTE_OP_RETURN_OUTPUT);
            stateService.setTxOutputType(blindVoteLockStakeOutput, TxOutputType.BLIND_VOTE_LOCK_STAKE_OUTPUT);
            txState.setVerifiedOpReturnType(OpReturnType.BLIND_VOTE);
        } else {
            log.info("We expected a blind vote op_return data but it did not " +
                    "match our rules. txOutput={}; blockHeight={}", txOutput, blockHeight);

            //TODO does it makes the tx invalid if we set opReturn type to INVALID_OUTPUT?
            stateService.setTxOutputType(txOutput, TxOutputType.INVALID_OUTPUT);

            // We don't want to burn the BlindVoteLockStakeOutput. We verified it at the output
            // iteration that it is valid BSQ so we set TxOutputType.BSQ_OUTPUT.
            if (blindVoteLockStakeOutput != null)
                stateService.setTxOutputType(blindVoteLockStakeOutput, TxOutputType.BSQ_OUTPUT);
        }
    }

    private void processVoteReveal(byte[] opReturnData, TxOutput txOutput, int blockHeight, TxState txState) {
        if (opReturnVoteRevealValidator.validate(opReturnData, blockHeight, txState)) {
            stateService.setTxOutputType(txOutput, TxOutputType.VOTE_REVEAL_OP_RETURN_OUTPUT);
            stateService.setTxOutputType(txState.getVoteRevealUnlockStakeOutput(), TxOutputType.VOTE_REVEAL_UNLOCK_STAKE_OUTPUT);
            txState.setVerifiedOpReturnType(OpReturnType.VOTE_REVEAL);
        } else {
            log.info("We expected a vote reveal op_return data but it did not " +
                    "match our rules. txOutput={}; blockHeight={}", txOutput, blockHeight);

            //TODO does it makes the tx invalid if we set opReturn type to INVALID_OUTPUT?
            stateService.setTxOutputType(txOutput, TxOutputType.INVALID_OUTPUT);

            // We don't want to burn the VoteRevealUnlockStakeOutput. We verified it at the output iteration
            // that it is valid BSQ.
            if (txState.getVoteRevealUnlockStakeOutput() != null)
                stateService.setTxOutputType(txState.getVoteRevealUnlockStakeOutput(), TxOutputType.BSQ_OUTPUT);
        }
    }
}
