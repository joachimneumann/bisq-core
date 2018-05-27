/*
 * This file is part of bisq.
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

package bisq.core.dao.voting.myvote;

import bisq.core.app.BisqEnvironment;
import bisq.core.dao.state.StateService;
import bisq.core.dao.voting.ballot.BallotList;
import bisq.core.dao.voting.blindvote.BlindVote;

import bisq.common.crypto.Encryption;
import bisq.common.proto.persistable.PersistedDataHost;
import bisq.common.storage.Storage;

import javax.inject.Inject;

import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates and stores myVote items. Persist in MyVoteList.
 */
@Slf4j
public class MyVoteListService implements PersistedDataHost {
    private final StateService stateService;
    private final Storage<MyVoteList> storage;
    private final MyVoteList myVoteList = new MyVoteList();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public MyVoteListService(StateService stateService,
                             Storage<MyVoteList> storage) {
        this.stateService = stateService;
        this.storage = storage;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PersistedDataHost
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void readPersisted() {
        if (BisqEnvironment.isDAOActivatedAndBaseCurrencySupportingBsq()) {
            MyVoteList persisted = storage.initAndGetPersisted(myVoteList, 100);
            if (persisted != null) {
                this.myVoteList.clear();
                this.myVoteList.addAll(persisted.getList());
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void start() {
    }

    public void createAndAddMyVote(BallotList sortedBallotListForCycle, SecretKey secretKey, BlindVote blindVote) {
        final byte[] secretKeyBytes = Encryption.getSecretKeyBytes(secretKey);
        MyVote myVote = new MyVote(stateService.getChainHeight(), sortedBallotListForCycle, secretKeyBytes, blindVote);
        log.info("Add new MyVote to myVotesList list.\nMyVote=" + myVote);
        myVoteList.add(myVote);
        persist();
    }

    public void applyRevealTxId(MyVote myVote, String voteRevealTxId) {
        myVote.setRevealTxId(voteRevealTxId);
        log.info("Applied revealTxId to myVote.\nmyVote={}\nvoteRevealTxId={}", myVote, voteRevealTxId);
        persist();
    }

    public MyVoteList getMyVoteList() {
        return myVoteList;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void persist() {
        storage.queueUpForSave();
    }
}