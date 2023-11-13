/*
 * Copyright Consensys Software Inc., 2023
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

package tech.pegasys.teku.statetransition.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.spec.datastructures.validator.BroadcastValidationLevel.CONSENSUS_AND_EQUIVOCATION;
import static tech.pegasys.teku.spec.datastructures.validator.BroadcastValidationLevel.GOSSIP;
import static tech.pegasys.teku.spec.datastructures.validator.BroadcastValidationLevel.NOT_REQUIRED;
import static tech.pegasys.teku.statetransition.validation.BlockBroadcastValidator.BroadcastValidationResult.CONSENSUS_FAILURE;
import static tech.pegasys.teku.statetransition.validation.BlockBroadcastValidator.BroadcastValidationResult.FINAL_EQUIVOCATION_FAILURE;
import static tech.pegasys.teku.statetransition.validation.BlockBroadcastValidator.BroadcastValidationResult.GOSSIP_FAILURE;
import static tech.pegasys.teku.statetransition.validation.BlockBroadcastValidator.BroadcastValidationResult.SUCCESS;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.spec.datastructures.validator.BroadcastValidationLevel;
import tech.pegasys.teku.spec.logic.common.statetransition.results.BlockImportResult;
import tech.pegasys.teku.spec.util.DataStructureUtil;

public class BlockBroadcastValidatorTest {
  private final Spec spec = TestSpecFactory.createMinimalPhase0();
  private final DataStructureUtil dataStructureUtil = new DataStructureUtil(spec);
  private final SignedBeaconBlock block = dataStructureUtil.randomSignedBeaconBlock();

  private final BlockGossipValidator blockGossipValidator = mock(BlockGossipValidator.class);

  private BlockBroadcastValidator blockBroadcastValidator;

  final SafeFuture<BlockImportResult> blockImportResult = new SafeFuture<>();

  @Test
  public void shouldReturnSuccessWhenValidationIsGossipAndGossipValidationReturnsAccept() {
    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(InternalValidationResult.ACCEPT));

    prepareBlockBroadcastValidator(GOSSIP);

    assertThat(blockBroadcastValidator.getResult())
        .isCompletedWithValueMatching(result -> result.equals(SUCCESS));
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  @Test
  public void
      shouldReturnSuccessWhenValidationIsGossipAndGossipValidationReturnsAcceptEvenWhenBlockImportFails() {
    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(InternalValidationResult.ACCEPT));

    blockImportResult.completeExceptionally(new RuntimeException("error"));

    prepareBlockBroadcastValidator(GOSSIP);

    assertThat(blockBroadcastValidator.getResult())
        .isCompletedWithValueMatching(result -> result.equals(SUCCESS));
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  @ParameterizedTest
  @MethodSource("provideBroadcastValidationsAndGossipFailures")
  public void shouldReturnGossipFailureImmediatelyWhenGossipValidationIsNotAccept(
      final BroadcastValidationLevel broadcastValidation,
      final InternalValidationResult internalValidationResult) {

    if (broadcastValidation == NOT_REQUIRED) {
      prepareBlockBroadcastValidator(broadcastValidation);

      assertThat(blockBroadcastValidator.getResult())
          .isCompletedWithValueMatching(result -> result.equals(SUCCESS));
      verifyNoMoreInteractions(blockGossipValidator);
      return;
    }

    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(internalValidationResult));

    prepareBlockBroadcastValidator(broadcastValidation);

    // consensus validation success should not affect the result
    blockBroadcastValidator.onConsensusValidationSucceeded();

    assertThat(blockBroadcastValidator.getResult())
        .isCompletedWithValueMatching(result -> result.equals(GOSSIP_FAILURE));
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  @ParameterizedTest
  @EnumSource(
      value = BroadcastValidationLevel.class,
      names = {"CONSENSUS", "CONSENSUS_AND_EQUIVOCATION"})
  public void shouldReturnConsensusFailureImmediatelyWhenConsensusValidationIsNotSuccessful(
      final BroadcastValidationLevel broadcastValidation) {

    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(InternalValidationResult.ACCEPT));

    prepareBlockBroadcastValidator(broadcastValidation);

    blockImportResult.complete(
        BlockImportResult.failedStateTransition(new RuntimeException("error")));

    assertThat(blockBroadcastValidator.getResult())
        .isCompletedWithValueMatching(result -> result.equals(CONSENSUS_FAILURE));
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  @ParameterizedTest
  @EnumSource(
      value = BroadcastValidationLevel.class,
      names = {"CONSENSUS", "CONSENSUS_AND_EQUIVOCATION"})
  public void shouldReturnConsensusFailureImmediatelyWhenConsensusCompleteExceptionally(
      final BroadcastValidationLevel broadcastValidation) {
    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(InternalValidationResult.ACCEPT));

    prepareBlockBroadcastValidator(broadcastValidation);

    blockImportResult.completeExceptionally(new RuntimeException("error"));

    assertThat(blockBroadcastValidator.getResult()).isCompletedExceptionally();
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  @Test
  public void shouldReturnSuccessWhenSecondEquivocationCheckIsValidated() {
    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(InternalValidationResult.ACCEPT));

    when(blockGossipValidator.blockIsFirstBlockWithValidSignatureForSlot(eq(block)))
        .thenReturn(Boolean.TRUE);

    prepareBlockBroadcastValidator(CONSENSUS_AND_EQUIVOCATION);

    assertThat(blockBroadcastValidator.getResult()).isNotDone();

    blockBroadcastValidator.onConsensusValidationSucceeded();

    // any subsequent failures won't affect the result
    blockImportResult.completeExceptionally(new RuntimeException("error"));

    assertThat(blockBroadcastValidator.getResult())
        .isCompletedWithValueMatching(result -> result.equals(SUCCESS));
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verify(blockGossipValidator).blockIsFirstBlockWithValidSignatureForSlot(eq(block));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  @Test
  public void shouldReturnFinalEquivocationFailureWhenSecondEquivocationCheckFails() {
    when(blockGossipValidator.validate(eq(block), eq(true)))
        .thenReturn(SafeFuture.completedFuture(InternalValidationResult.ACCEPT));

    when(blockGossipValidator.blockIsFirstBlockWithValidSignatureForSlot(eq(block)))
        .thenReturn(Boolean.FALSE);

    prepareBlockBroadcastValidator(CONSENSUS_AND_EQUIVOCATION);

    blockBroadcastValidator.onConsensusValidationSucceeded();

    assertThat(blockBroadcastValidator.getResult())
        .isCompletedWithValueMatching(result -> result.equals(FINAL_EQUIVOCATION_FAILURE));
    verify(blockGossipValidator).validate(eq(block), eq(true));
    verify(blockGossipValidator).blockIsFirstBlockWithValidSignatureForSlot(eq(block));
    verifyNoMoreInteractions(blockGossipValidator);
  }

  private static Stream<Arguments> provideBroadcastValidationsAndGossipFailures() {
    return Arrays.stream(BroadcastValidationLevel.values())
        .flatMap(
            broadcastValidation ->
                Stream.of(
                    Arguments.of(broadcastValidation, InternalValidationResult.IGNORE),
                    Arguments.of(broadcastValidation, InternalValidationResult.SAVE_FOR_FUTURE)));
  }

  private void prepareBlockBroadcastValidator(
      final BroadcastValidationLevel broadcastValidationLevel) {
    blockBroadcastValidator =
        BlockBroadcastValidator.create(block, blockGossipValidator, broadcastValidationLevel);

    blockBroadcastValidator.attachToBlockImport(blockImportResult);
  }
}
