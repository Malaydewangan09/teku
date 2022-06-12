/*
 * Copyright ConsenSys Software Inc., 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.storage.server.kvstore.dataaccess;

import com.google.errorprone.annotations.MustBeClosed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.ethereum.pow.api.DepositsFromBlockEvent;
import tech.pegasys.teku.ethereum.pow.api.MinGenesisTimeBlockEvent;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.blocks.BlockAndCheckpointEpochs;
import tech.pegasys.teku.spec.datastructures.blocks.CheckpointEpochs;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.SlotAndBlockRoot;
import tech.pegasys.teku.spec.datastructures.forkchoice.VoteTracker;
import tech.pegasys.teku.spec.datastructures.state.Checkpoint;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.storage.server.kvstore.ColumnEntry;
import tech.pegasys.teku.storage.server.kvstore.schema.KvStoreColumn;
import tech.pegasys.teku.storage.server.kvstore.schema.KvStoreVariable;

public class KvStoreCombinedDaoAdapter implements KvStoreCombinedDao, V4MigratableSourceDao {
  private final V4HotKvStoreDao hotDao;
  private final V4FinalizedKvStoreDao finalizedDao;

  public KvStoreCombinedDaoAdapter(
      final V4HotKvStoreDao hotDao, final V4FinalizedKvStoreDao finalizedDao) {
    this.hotDao = hotDao;
    this.finalizedDao = finalizedDao;
  }

  @Override
  public Optional<UInt64> getGenesisTime() {
    return hotDao.getGenesisTime();
  }

  @Override
  public Optional<Checkpoint> getAnchor() {
    return hotDao.getAnchor();
  }

  @Override
  public Optional<Checkpoint> getJustifiedCheckpoint() {
    return hotDao.getJustifiedCheckpoint();
  }

  @Override
  public Optional<Checkpoint> getBestJustifiedCheckpoint() {
    return hotDao.getBestJustifiedCheckpoint();
  }

  @Override
  public Optional<Checkpoint> getFinalizedCheckpoint() {
    return hotDao.getFinalizedCheckpoint();
  }

  @Override
  public Optional<BeaconState> getLatestFinalizedState() {
    return hotDao.getLatestFinalizedState();
  }

  @Override
  public Optional<Checkpoint> getWeakSubjectivityCheckpoint() {
    return hotDao.getWeakSubjectivityCheckpoint();
  }

  @Override
  public Optional<SignedBeaconBlock> getHotBlock(final Bytes32 root) {
    return hotDao.getHotBlock(root);
  }

  @Override
  public Optional<CheckpointEpochs> getHotBlockCheckpointEpochs(final Bytes32 root) {
    return hotDao.getHotBlockCheckpointEpochs(root);
  }

  @Override
  public Optional<BeaconState> getHotState(final Bytes32 root) {
    return hotDao.getHotState(root);
  }

  @Override
  public List<Bytes32> getStateRootsBeforeSlot(final UInt64 slot) {
    return hotDao.getStateRootsBeforeSlot(slot);
  }

  @Override
  public Optional<SlotAndBlockRoot> getSlotAndBlockRootFromStateRoot(final Bytes32 stateRoot) {
    return hotDao.getSlotAndBlockRootFromStateRoot(stateRoot);
  }

  @Override
  @MustBeClosed
  public Stream<SignedBeaconBlock> streamHotBlocks() {
    return hotDao.streamHotBlocks();
  }

  @Override
  public Map<UInt64, VoteTracker> getVotes() {
    return hotDao.getVotes();
  }

  @Override
  @MustBeClosed
  public HotUpdater hotUpdater() {
    return hotDao.hotUpdater();
  }

  @Override
  public Optional<SignedBeaconBlock> getFinalizedBlock(final Bytes32 root) {
    return finalizedDao.getFinalizedBlock(root);
  }

  @Override
  @MustBeClosed
  public FinalizedUpdater finalizedUpdater() {
    return finalizedDao.finalizedUpdater();
  }

  @Override
  public Optional<SignedBeaconBlock> getFinalizedBlockAtSlot(final UInt64 slot) {
    return finalizedDao.getFinalizedBlockAtSlot(slot);
  }

  @Override
  public Optional<UInt64> getEarliestFinalizedBlockSlot() {
    return finalizedDao.getEarliestFinalizedBlockSlot();
  }

  @Override
  public Optional<SignedBeaconBlock> getEarliestFinalizedBlock() {
    return finalizedDao.getEarliestFinalizedBlock();
  }

  @Override
  public Optional<SignedBeaconBlock> getLatestFinalizedBlockAtSlot(final UInt64 slot) {
    return finalizedDao.getLatestFinalizedBlockAtSlot(slot);
  }

  @Override
  public List<SignedBeaconBlock> getNonCanonicalBlocksAtSlot(final UInt64 slot) {
    return finalizedDao.getNonCanonicalBlocksAtSlot(slot);
  }

  @Override
  public Optional<BeaconState> getLatestAvailableFinalizedState(final UInt64 maxSlot) {
    return finalizedDao.getLatestAvailableFinalizedState(maxSlot);
  }

  @Override
  @MustBeClosed
  public Stream<SignedBeaconBlock> streamFinalizedBlocks(
      final UInt64 startSlot, final UInt64 endSlot) {
    return finalizedDao.streamFinalizedBlocks(startSlot, endSlot);
  }

  @Override
  public Optional<UInt64> getSlotForFinalizedBlockRoot(final Bytes32 blockRoot) {
    return finalizedDao.getSlotForFinalizedBlockRoot(blockRoot);
  }

  @Override
  public Optional<UInt64> getSlotForFinalizedStateRoot(final Bytes32 stateRoot) {
    return finalizedDao.getSlotForFinalizedStateRoot(stateRoot);
  }

  @Override
  public Optional<SlotAndBlockRoot> getSlotAndBlockRootForFinalizedStateRoot(
      final Bytes32 stateRoot) {
    return finalizedDao.getSlotAndBlockRootForFinalizedStateRoot(stateRoot);
  }

  @Override
  public Optional<UInt64> getOptimisticTransitionBlockSlot() {
    return finalizedDao.getOptimisticTransitionBlockSlot();
  }

  @Override
  public Optional<? extends SignedBeaconBlock> getNonCanonicalBlock(final Bytes32 root) {
    return finalizedDao.getNonCanonicalBlock(root);
  }

  @Override
  public void ingest(
      final KvStoreCombinedDao dao, final int batchSize, final Consumer<String> logger) {
    throw new UnsupportedOperationException("Cannot migrate to a split database format");
  }

  @Override
  @MustBeClosed
  public Stream<DepositsFromBlockEvent> streamDepositsFromBlocks() {
    return hotDao.streamDepositsFromBlocks();
  }

  @Override
  public Optional<MinGenesisTimeBlockEvent> getMinGenesisTimeBlock() {
    return hotDao.getMinGenesisTimeBlock();
  }

  @Override
  public void close() throws Exception {
    hotDao.close();
    hotDao.close();
    finalizedDao.close();
  }

  @Override
  @MustBeClosed
  @SuppressWarnings("MustBeClosedChecker")
  public CombinedUpdater combinedUpdater() {
    return new CombinedUpdaterAdapter(hotUpdater(), finalizedUpdater());
  }

  @Override
  public Map<String, KvStoreColumn<?, ?>> getColumnMap() {
    final Map<String, KvStoreColumn<?, ?>> allColumns = new HashMap<>();
    allColumns.putAll(hotDao.getColumnMap());
    allColumns.putAll(finalizedDao.getColumnMap());
    return allColumns;
  }

  @Override
  public Map<String, KvStoreVariable<?>> getVariableMap() {
    final Map<String, KvStoreVariable<?>> allVariables = new HashMap<>();
    allVariables.putAll(hotDao.getVariableMap());
    allVariables.putAll(finalizedDao.getVariableMap());
    return allVariables;
  }

  @Override
  public <T> Optional<Bytes> getRawVariable(final KvStoreVariable<T> var) {
    if (hotDao.getVariableMap().containsValue(var)) {
      return hotDao.getRawVariable(var);
    } else {
      return finalizedDao.getRawVariable(var);
    }
  }

  @Override
  @MustBeClosed
  public <K, V> Stream<ColumnEntry<Bytes, Bytes>> streamRawColumn(
      final KvStoreColumn<K, V> kvStoreColumn) {
    if (hotDao.getColumnMap().containsValue(kvStoreColumn)) {
      return hotDao.streamRawColumn(kvStoreColumn);
    } else {
      return finalizedDao.streamRawColumn(kvStoreColumn);
    }
  }

  private static class CombinedUpdaterAdapter implements CombinedUpdater {
    private final HotUpdater hotUpdater;
    private final FinalizedUpdater finalizedUpdater;

    private CombinedUpdaterAdapter(
        final HotUpdater hotUpdater, final FinalizedUpdater finalizedUpdater) {
      this.hotUpdater = hotUpdater;
      this.finalizedUpdater = finalizedUpdater;
    }

    @Override
    public void setGenesisTime(final UInt64 genesisTime) {
      hotUpdater.setGenesisTime(genesisTime);
    }

    @Override
    public void setAnchor(final Checkpoint anchorPoint) {
      hotUpdater.setAnchor(anchorPoint);
    }

    @Override
    public void setJustifiedCheckpoint(final Checkpoint checkpoint) {
      hotUpdater.setJustifiedCheckpoint(checkpoint);
    }

    @Override
    public void setBestJustifiedCheckpoint(final Checkpoint checkpoint) {
      hotUpdater.setBestJustifiedCheckpoint(checkpoint);
    }

    @Override
    public void setFinalizedCheckpoint(final Checkpoint checkpoint) {
      hotUpdater.setFinalizedCheckpoint(checkpoint);
    }

    @Override
    public void setWeakSubjectivityCheckpoint(final Checkpoint checkpoint) {
      hotUpdater.setWeakSubjectivityCheckpoint(checkpoint);
    }

    @Override
    public void clearWeakSubjectivityCheckpoint() {
      hotUpdater.clearWeakSubjectivityCheckpoint();
    }

    @Override
    public void setLatestFinalizedState(final BeaconState state) {
      hotUpdater.setLatestFinalizedState(state);
    }

    @Override
    public void addHotBlock(final BlockAndCheckpointEpochs blockAndCheckpointEpochs) {
      hotUpdater.addHotBlock(blockAndCheckpointEpochs);
    }

    @Override
    public void addHotBlockCheckpointEpochs(
        final Bytes32 blockRoot, final CheckpointEpochs checkpointEpochs) {
      hotUpdater.addHotBlockCheckpointEpochs(blockRoot, checkpointEpochs);
    }

    @Override
    public void addHotState(final Bytes32 blockRoot, final BeaconState state) {
      hotUpdater.addHotState(blockRoot, state);
    }

    @Override
    public void addHotStates(final Map<Bytes32, BeaconState> states) {
      hotUpdater.addHotStates(states);
    }

    @Override
    public void addVotes(final Map<UInt64, VoteTracker> states) {
      hotUpdater.addVotes(states);
    }

    @Override
    public void addHotBlocks(final Map<Bytes32, BlockAndCheckpointEpochs> blocks) {
      hotUpdater.addHotBlocks(blocks);
    }

    @Override
    public void addHotStateRoots(
        final Map<Bytes32, SlotAndBlockRoot> stateRootToSlotAndBlockRootMap) {
      hotUpdater.addHotStateRoots(stateRootToSlotAndBlockRootMap);
    }

    @Override
    public void pruneHotStateRoots(final List<Bytes32> stateRoots) {
      hotUpdater.pruneHotStateRoots(stateRoots);
    }

    @Override
    public void deleteHotBlock(final Bytes32 blockRoot) {
      hotUpdater.deleteHotBlock(blockRoot);
    }

    @Override
    public void deleteHotState(final Bytes32 blockRoot) {
      hotUpdater.deleteHotState(blockRoot);
    }

    @Override
    public void addFinalizedBlock(final SignedBeaconBlock block) {
      finalizedUpdater.addFinalizedBlock(block);
    }

    @Override
    public void addNonCanonicalBlock(final SignedBeaconBlock block) {
      finalizedUpdater.addNonCanonicalBlock(block);
    }

    @Override
    public void addNonCanonicalRootAtSlot(final UInt64 slot, final Set<Bytes32> blockRoots) {
      finalizedUpdater.addNonCanonicalRootAtSlot(slot, blockRoots);
    }

    @Override
    public void addFinalizedState(final Bytes32 blockRoot, final BeaconState state) {
      finalizedUpdater.addFinalizedState(blockRoot, state);
    }

    @Override
    public void addFinalizedStateRoot(final Bytes32 stateRoot, final UInt64 slot) {
      finalizedUpdater.addFinalizedStateRoot(stateRoot, slot);
    }

    @Override
    public void setOptimisticTransitionBlockSlot(final Optional<UInt64> transitionBlockSlot) {
      finalizedUpdater.setOptimisticTransitionBlockSlot(transitionBlockSlot);
    }

    @Override
    public void addMinGenesisTimeBlock(final MinGenesisTimeBlockEvent event) {
      hotUpdater.addMinGenesisTimeBlock(event);
    }

    @Override
    public void addDepositsFromBlockEvent(final DepositsFromBlockEvent event) {
      hotUpdater.addDepositsFromBlockEvent(event);
    }

    @Override
    public void commit() {
      finalizedUpdater.commit();
      hotUpdater.commit();
    }

    @Override
    public void cancel() {
      finalizedUpdater.cancel();
      hotUpdater.cancel();
    }

    @Override
    public void close() {
      finalizedUpdater.close();
      hotUpdater.close();
    }
  }
}
