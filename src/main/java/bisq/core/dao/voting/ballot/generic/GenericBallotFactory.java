/*
 * This file is part of Bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.dao.voting.ballot.generic;

import lombok.extern.slf4j.Slf4j;

//TODO impl
@Slf4j
public class GenericBallotFactory {
    /*private final BsqWalletService bsqWalletService;
    private final BtcWalletService btcWalletService;
    private final ChangeParamListService changeParamService;
    private final ProposalValidator proposalPayloadValidator;
    private final PublicKey signaturePubKey;
    private final StateService stateService;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public GenericBallotFactory(BsqWalletService bsqWalletService,
                                  BtcWalletService btcWalletService,
                                  StateService stateService,
                                  ChangeParamListService changeParamService,
                                  ProposalValidator proposalPayloadValidator,
                                  KeyRing keyRing) {
        this.bsqWalletService = bsqWalletService;
        this.btcWalletService = btcWalletService;
        this.stateService = stateService;
        this.changeParamService = changeParamService;
        this.proposalPayloadValidator = proposalPayloadValidator;

        signaturePubKey = keyRing.getPubKeyRing().getSignaturePubKey();
    }

    public Tuple2<Ballot, Transaction> makeTxAndGetGenericProposal(String name,
                                                                   String title,
                                                                   String description,
                                                                   String link)
            throws ValidationException, InsufficientMoneyException, IOException, TransactionVerificationException,
            WalletException {

        // As we don't know the txId we create a temp object with TxId set to null.
        final GenericProposal tempPayload = new GenericProposal(
                UUID.randomUUID().toString(),
                name,
                title,
                description,
                link,
                signaturePubKey,
                new Date()
        );
        proposalPayloadValidator.validateDataFields(tempPayload);

        Transaction transaction = createGenericProposalTx(tempPayload);

        return new Tuple2<>(createGenericProposal(tempPayload, transaction), transaction);
    }

    // We have txId set to null in tempPayload as we cannot know it before the tx is created.
    // Once the tx is known we will create a new object including the txId.
    // The hashOfPayload used in the opReturnData is created with the txId set to null.
    private Transaction createGenericProposalTx(GenericProposal tempPayload)
            throws InsufficientMoneyException, TransactionVerificationException, WalletException, IOException {

        final Coin fee = ProposalConsensus.getFee(changeParamService, stateService.getChainHeight());
        final Transaction preparedBurnFeeTx = bsqWalletService.getPreparedBurnFeeTx(fee);

        // payload does not have txId at that moment
        byte[] hashOfPayload = ProposalConsensus.getHashOfPayload(tempPayload);
        byte[] opReturnData = ProposalConsensus.getOpReturnData(hashOfPayload);

        final Transaction txWithBtcFee = btcWalletService.completePreparedGenericProposalTx(
                preparedBurnFeeTx,
                opReturnData);

        final Transaction completedTx = bsqWalletService.signTx(txWithBtcFee);
        log.info("GenericBallot tx: " + completedTx);
        return completedTx;
    }

    // We have txId set to null in tempPayload as we cannot know it before the tx is created.
    // Once the tx is known we will create a new object including the txId.
    private GenericBallot createGenericProposal(GenericProposal tempPayload, Transaction transaction) {
        final String txId = transaction.getHashAsString();
        GenericProposal compensationRequestPayload = (GenericProposal) tempPayload.cloneWithTxId(txId);
        return new GenericBallot(compensationRequestPayload);
    }*/
}