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

package bisq.core.dao.voting.proposal;

import bisq.core.dao.state.BlockListener;
import bisq.core.dao.state.ParseBlockChainListener;
import bisq.core.dao.state.StateService;
import bisq.core.dao.state.blockchain.Block;
import bisq.core.dao.state.period.DaoPhase;
import bisq.core.dao.state.period.PeriodService;
import bisq.core.dao.voting.proposal.storage.appendonly.ProposalPayload;
import bisq.core.dao.voting.proposal.storage.appendonly.ProposalStorageService;
import bisq.core.dao.voting.proposal.storage.temp.TempProposalPayload;
import bisq.core.dao.voting.proposal.storage.temp.TempProposalStorageService;

import bisq.network.p2p.P2PService;
import bisq.network.p2p.storage.HashMapChangedListener;
import bisq.network.p2p.storage.payload.PersistableNetworkPayload;
import bisq.network.p2p.storage.payload.ProtectedStorageEntry;
import bisq.network.p2p.storage.payload.ProtectedStoragePayload;
import bisq.network.p2p.storage.persistence.AppendOnlyDataStoreListener;
import bisq.network.p2p.storage.persistence.AppendOnlyDataStoreService;
import bisq.network.p2p.storage.persistence.ProtectedDataStoreService;

import com.google.inject.Inject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Maintains protectedStoreList and protectedStoreList.
 * Republishes protectedStoreList to append-only data store when entering the break before the blind vote phase.
 */
@Slf4j
public class ProposalService implements HashMapChangedListener, AppendOnlyDataStoreListener,
        BlockListener, ParseBlockChainListener {

    private final P2PService p2PService;
    private final PeriodService periodService;
    private final StateService stateService;
    private final ProposalValidator proposalValidator;

    // Proposals we receive in the proposal phase. They can be removed in that phase. That list must not be used for
    // consensus critical code.
    @Getter
    private final ObservableList<Proposal> protectedStoreList = FXCollections.observableArrayList();

    // Proposals which got added to the append-only data store in the break before the blind vote phase.
    // They cannot be removed anymore. This list is used for consensus critical code. Different nodes might have
    // different data collections due the eventually consistency of the P2P network.
    @Getter
    private final ObservableList<ProposalPayload> appendOnlyStoreList = FXCollections.observableArrayList();

    private boolean parsingComplete;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public ProposalService(P2PService p2PService,
                           PeriodService periodService,
                           ProposalStorageService proposalStorageService,
                           TempProposalStorageService tempProposalStorageService,
                           AppendOnlyDataStoreService appendOnlyDataStoreService,
                           ProtectedDataStoreService protectedDataStoreService,
                           StateService stateService,
                           ProposalValidator proposalValidator) {
        this.p2PService = p2PService;
        this.periodService = periodService;
        this.stateService = stateService;
        this.proposalValidator = proposalValidator;

        appendOnlyDataStoreService.addService(proposalStorageService);
        protectedDataStoreService.addService(tempProposalStorageService);

        stateService.addParseBlockChainListener(this);
        stateService.addBlockListener(this);

        p2PService.addHashSetChangedListener(this);
        p2PService.getP2PDataStorage().addAppendOnlyDataStoreListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void start() {
        fillListFromAppendOnlyDataStore();
        fillListFromProtectedStore();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // ParseBlockChainListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onComplete() {
        parsingComplete = true;

        stateService.removeParseBlockChainListener(this);

        // Fill the lists with the data we have collected in out stores.
        fillListFromProtectedStore();
        fillListFromAppendOnlyDataStore();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // BlockListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onBlockAdded(Block block) {
        // We need to do that after parsing the block to have the txs available
        // We will use the 10th block of break1 phase to avoid re-org issues.
        int heightForRepublishing = periodService.getFirstBlockOfPhase(stateService.getChainHeight(), DaoPhase.Phase.BREAK1) + 1 /* + 9*/;
        if (block.getHeight() == heightForRepublishing) {
            // We only republish if we are not still parsing old blocks
            if (parsingComplete) {
                // We use first block of break1 for the block hash
                final int heightOfFirstBlockOfBreak1 = periodService.getFirstBlockOfPhase(stateService.getChainHeight(), DaoPhase.Phase.BREAK1);
                stateService.getBlockAtHeight(heightOfFirstBlockOfBreak1)
                        .ifPresent(firstBlockInBreak -> publishToAppendOnlyDataStore(firstBlockInBreak.getHash()));
            }

            fillListFromAppendOnlyDataStore();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // HashMapChangedListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAdded(ProtectedStorageEntry entry) {
        onProtectedDataAdded(entry);
    }

    @Override
    public void onRemoved(ProtectedStorageEntry entry) {
        onProtectedDataRemoved(entry);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // AppendOnlyDataStoreListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAdded(PersistableNetworkPayload payload) {
        onAppendOnlyDataAdded(payload);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void fillListFromProtectedStore() {
        p2PService.getDataMap().values().forEach(this::onProtectedDataAdded);
    }

    private void fillListFromAppendOnlyDataStore() {
        p2PService.getP2PDataStorage().getAppendOnlyDataStoreMap().values().forEach(this::onAppendOnlyDataAdded);
    }


    private void publishToAppendOnlyDataStore(String blockHash) {
        protectedStoreList.stream()
                .filter(proposalValidator::isValidAndConfirmed)
                .map(proposal -> new ProposalPayload(proposal, blockHash))
                .forEach(appendOnlyPayload -> {
                    boolean success = p2PService.addPersistableNetworkPayload(appendOnlyPayload, true);
                    if (!success)
                        log.warn("publishToAppendOnlyDataStore failed for proposal " + appendOnlyPayload.getProposal());
                });
    }

    private void onProtectedDataAdded(ProtectedStorageEntry entry) {
        final ProtectedStoragePayload protectedStoragePayload = entry.getProtectedStoragePayload();
        if (protectedStoragePayload instanceof TempProposalPayload) {
            final Proposal proposal = ((TempProposalPayload) protectedStoragePayload).getProposal();

            // We do not validate phase, cycle and confirmation yet as the tx might be not available/confirmed yet.
            // Though we check if we are in the proposal phase. During parsing we might miss proposals but we will
            // handle that in the node to request again the p2p network data so we get added potentially missed data
            if (proposalValidator.isValidOrUnconfirmed(proposal)) {
                if (!protectedStoreList.contains(proposal))
                    protectedStoreList.add(proposal);
            } else {
                //TODO called at startup when we are not in cycle of proposal
                log.debug("We received a invalid proposal from the P2P network. Proposal.txId={}, blockHeight={}",
                        proposal.getTxId(), stateService.getChainHeight());
            }
        }
    }

    private void onProtectedDataRemoved(ProtectedStorageEntry entry) {
        final ProtectedStoragePayload protectedStoragePayload = entry.getProtectedStoragePayload();
        if (protectedStoragePayload instanceof TempProposalPayload) {
            final Proposal proposal = ((TempProposalPayload) protectedStoragePayload).getProposal();
            // We allow removal only if we are in the proposal phase.
            if (periodService.isInPhase(stateService.getChainHeight(), DaoPhase.Phase.PROPOSAL)) {
                if (protectedStoreList.contains(proposal))
                    protectedStoreList.remove(proposal);
            } else {
                log.warn("We received a remove request outside the PROPOSAL phase. " +
                        "Proposal.txId={}, blockHeight={}", proposal.getTxId(), stateService.getChainHeight());
            }
        }
    }

    private void onAppendOnlyDataAdded(PersistableNetworkPayload persistableNetworkPayload) {
        if (persistableNetworkPayload instanceof ProposalPayload) {
            ProposalPayload proposalPayload = (ProposalPayload) persistableNetworkPayload;
            int blockHeightOfBreakStart = periodService.getFirstBlockOfPhase(stateService.getChainHeight(), DaoPhase.Phase.BREAK1);
            if (proposalValidator.hasCorrectBlockHash(proposalPayload, blockHeightOfBreakStart, stateService)) {
                if (proposalValidator.isValidAndConfirmed(proposalPayload.getProposal())) {
                    if (!appendOnlyStoreList.contains(proposalPayload))
                        appendOnlyStoreList.add(proposalPayload);
                } else {
                    log.warn("We received a invalid append-only proposal from the P2P network. " +
                                    "Proposal.txId={}, blockHeight={}",
                            proposalPayload.getProposal().getTxId(), stateService.getChainHeight());
                }
            } else {
                //TODO called at startup when we are not in cycle of proposal
                log.debug("We received an invalid proposalPayload. payload={}, blockHeightOfBreakStart={}",
                        proposalPayload, blockHeightOfBreakStart);
            }
        }
    }
}