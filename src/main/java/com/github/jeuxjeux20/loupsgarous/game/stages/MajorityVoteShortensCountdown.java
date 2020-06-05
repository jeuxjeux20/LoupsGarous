package com.github.jeuxjeux20.loupsgarous.game.stages;

import com.github.jeuxjeux20.loupsgarous.game.stages.interaction.Votable;
import com.github.jeuxjeux20.loupsgarous.game.stages.interaction.Votable.VoteState;
import org.checkerframework.common.value.qual.IntRange;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When annotated on a {@link LGGameStage} implementing {@link DualCountdownStage} and {@link Votable},
 * changes the timer of the {@linkplain DualCountdownStage#getCountdown() countdown} to
 * {@link #timeLeft()} when {@linkplain VoteState#getPlayerWithMostVotes() the player with the most votes}
 * holds the same or more vote share than the {@link #majorityPercentage()}.
 * <p>
 * However, if some votes change and the majority becomes invalid, the
 * {@linkplain DualCountdownStage#getCountdown() countdown}'s timer reverts back
 * to {@linkplain DualCountdownStage#getUnmodifiedCountdown() unmodified countdown}'s timer.
 * <p>
 * <b>Note:</b> This doesn't apply if the
 * {@linkplain DualCountdownStage#getUnmodifiedCountdown() unmodified countdown}'s timer
 * is lower than {@link #timeLeft()}.
 * <p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MajorityVoteShortensCountdown {
    @IntRange(from = 0, to = 100)
    int majorityPercentage() default 60;

    int timeLeft() default 30;
}