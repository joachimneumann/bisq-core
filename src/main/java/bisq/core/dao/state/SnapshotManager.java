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

package bisq.core.dao.state;

import bisq.core.dao.blockchain.vo.BsqBlock;

import bisq.common.proto.persistable.PersistenceProtoResolver;
import bisq.common.storage.Storage;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages snapshots of the ChainStateService.
 */
//TODO add tests; check if current logic is correct.
@Slf4j
public class SnapshotManager implements ChainStateService.Listener {
    private static final int SNAPSHOT_GRID = 10000;

    private final ChainState chainState;
    private final ChainStateService chainStateService;
    private final Storage<ChainState> storage;

    private ChainState snapshotCandidate;

    @Inject
    public SnapshotManager(ChainState chainState,
                           ChainStateService chainStateService,
                           PersistenceProtoResolver persistenceProtoResolver,
                           @Named(Storage.STORAGE_DIR) File storageDir) {
        this.chainState = chainState;
        this.chainStateService = chainStateService;
        storage = new Storage<>(storageDir, persistenceProtoResolver);

        chainStateService.addListener(this);
    }

    public void applySnapshot() {
        checkNotNull(storage, "storage must not be null");
        ChainState persisted = storage.initAndGetPersisted(chainState, 100);
        if (persisted != null) {
            log.info("applySnapshot persisted.chainHeadHeight=" + persisted.getBsqBlocks().getLast().getHeight());
            chainStateService.applySnapshot(persisted);
        } else {
            log.info("Try to apply snapshot but no stored snapshot available");
        }
    }

    @Override
    public void onBlockAdded(BsqBlock bsqBlock) {
        final int chainHeadHeight = chainStateService.getChainHeadHeight();
        if (isSnapshotHeight(chainHeadHeight) &&
                (snapshotCandidate == null ||
                        snapshotCandidate.getChainHeadHeight() != chainHeadHeight)) {
            // At trigger event we store the latest snapshotCandidate to disc
            if (snapshotCandidate != null) {
                // We clone because storage is in a threaded context
                final ChainState cloned = chainState.getClone(snapshotCandidate);
                storage.queueUpForSave(cloned);
                log.info("Saved snapshotCandidate to Disc at height " + chainHeadHeight);
            }
            // Now we clone and keep it in memory for the next trigger
            snapshotCandidate = chainState.getClone();
            // don't access cloned anymore with methods as locks are transient!
            log.debug("Cloned new snapshotCandidate at height " + chainHeadHeight);
        }
    }

    @VisibleForTesting
    int getSnapshotHeight(int genesisHeight, int height, int grid) {
        return Math.round(Math.max(genesisHeight + 3 * grid, height) / grid) * grid - grid;
    }

    @VisibleForTesting
    boolean isSnapshotHeight(int genesisHeight, int height, int grid) {
        return height % grid == 0 && height >= getSnapshotHeight(genesisHeight, height, grid);
    }

    private boolean isSnapshotHeight(int height) {
        return isSnapshotHeight(chainStateService.getGenesisBlockHeight(), height, SNAPSHOT_GRID);
    }

}