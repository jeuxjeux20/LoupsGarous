package com.github.jeuxjeux20.loupsgarous.game;

import com.github.jeuxjeux20.loupsgarous.LoupsGarous;
import com.github.jeuxjeux20.loupsgarous.ReactiveProperty;
import com.github.jeuxjeux20.loupsgarous.cards.CardDistributor;
import com.github.jeuxjeux20.loupsgarous.cards.LGCard;
import com.github.jeuxjeux20.loupsgarous.cards.VillageoisCard;
import com.github.jeuxjeux20.loupsgarous.cards.composition.Composition;
import com.github.jeuxjeux20.loupsgarous.cards.composition.ImmutableComposition;
import com.github.jeuxjeux20.loupsgarous.cards.composition.validation.CompositionValidator;
import com.github.jeuxjeux20.loupsgarous.endings.LGEnding;
import com.github.jeuxjeux20.loupsgarous.event.*;
import com.github.jeuxjeux20.loupsgarous.event.lobby.LGLobbyCompositionUpdateEvent;
import com.github.jeuxjeux20.loupsgarous.event.lobby.LGLobbyOwnerChangeEvent;
import com.github.jeuxjeux20.loupsgarous.event.player.LGPlayerJoinEvent;
import com.github.jeuxjeux20.loupsgarous.event.player.LGPlayerQuitEvent;
import com.github.jeuxjeux20.loupsgarous.extensibility.GameBundle;
import com.github.jeuxjeux20.loupsgarous.extensibility.ModBundle;
import com.github.jeuxjeux20.loupsgarous.extensibility.ModRegistry;
import com.github.jeuxjeux20.loupsgarous.kill.causes.PlayerQuitKillCause;
import com.github.jeuxjeux20.loupsgarous.lobby.*;
import com.github.jeuxjeux20.loupsgarous.phases.LGPhase;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import io.reactivex.rxjava3.core.Observable;
import me.lucko.helper.Events;
import me.lucko.helper.metadata.MetadataKey;
import me.lucko.helper.metadata.MetadataMap;
import me.lucko.helper.terminable.Terminable;
import me.lucko.helper.terminable.composite.CompositeClosingException;
import me.lucko.helper.terminable.composite.CompositeTerminable;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.github.jeuxjeux20.loupsgarous.LGChatStuff.*;
import static com.github.jeuxjeux20.loupsgarous.extensibility.LGExtensionPoints.CARDS;
import static com.github.jeuxjeux20.loupsgarous.extensibility.LGExtensionPoints.COMPOSITION_VALIDATORS;
import static com.github.jeuxjeux20.loupsgarous.game.LGGameState.*;

class MinecraftLGGameOrchestrator implements LGGameOrchestrator {
    // Terminables
    private final CompositeTerminable terminableRegistry = CompositeTerminable.create();
    // Base dependencies
    private final Injector injector;
    private final LoupsGarous plugin;
    private final OrchestratorLogger logger;
    private final LGGameManager gameManager;
    private final LobbyTeleporter lobbyTeleporter;
    private final OrchestratorScope scope;
    // Game state
    private final String id;
    private LGGameState state = LGGameState.UNINITIALIZED;

    private final Map<UUID, LGPlayer> players = new HashMap<>();
    private LGPlayer owner;
    private ImmutableComposition composition;
    private @Nullable CompositionValidator.Problem.Type worseCompositionProblemType;

    private final MutableLGGameTurn turn = new MutableLGGameTurn();
    private @Nullable LGEnding ending;

    private final ReactiveProperty<GameBundle> gameBundle = new ReactiveProperty<>();
    private final ReactiveProperty<ModBundle> modBundle = new ReactiveProperty<>();

    private final MetadataMap metadataMap = MetadataMap.create();
    // Components
    private DelayedDependencies delayedDependencies;
    private final Provider<DelayedDependencies> delayedDependenciesProvider;
    // Other stuff
    private final List<Runnable> postInitializationActions = new ArrayList<>();

    @Inject
    MinecraftLGGameOrchestrator(@Assisted LGGameBootstrapData bootstrapData,
                                Injector injector,
                                LoupsGarous plugin,
                                LobbyTeleporter.Factory lobbyTeleporterFactory,
                                LGGameManager gameManager,
                                OrchestratorScope scope,
                                ModRegistry modRegistry,
                                Provider<DelayedDependencies> delayedDependenciesProvider)
            throws GameCreationException {
        try {
            this.id = bootstrapData.getId();
            this.injector = injector;
            this.gameManager = gameManager;
            this.lobbyTeleporter = bind(lobbyTeleporterFactory.create());
            this.plugin = plugin;
            this.scope = scope;
            this.delayedDependenciesProvider = delayedDependenciesProvider;
            this.logger = new OrchestratorLogger();

            setModBundle(modRegistry.createDefaultBundle());
            setComposition(new ImmutableComposition(bootstrapData.getComposition()));

            try {
                setOwner(join(bootstrapData.getOwner()));
            } catch (PlayerJoinException e) {
                throw new InvalidOwnerException(e);
            }

            registerEventListeners();
        } catch (Throwable e) {
            terminableRegistry.closeAndReportException();
            throw e;
        }
    }

    @Override
    public void initialize() {
        this.getState().mustBe(UNINITIALIZED);

        try (OrchestratorScope.Block ignored = scope.use(this)) {
            delayedDependencies = delayedDependenciesProvider.get();
        }
        bind(delayedDependencies);

        updateLobbyStuff();

        postInitializationActions.forEach(Runnable::run);
        postInitializationActions.clear();

        if (phases().current() instanceof LGPhase.Null) {
            phases().next();
        }
    }

    @Override
    public void start() {
        getState().mustBe(READY_TO_START);

        new CardDistributor().distribute(composition, players.values());

        changeStateTo(STARTED, LGGameStartEvent::new);

        Events.call(new LGTurnChangeEvent(this));

        phases().next();
    }

    @Override
    public void finish(LGEnding ending) {
        getState().mustNotBe(UNINITIALIZED, FINISHED, DELETING, DELETED);

        this.ending = ending;

        changeStateTo(FINISHED, o -> new LGGameFinishedEvent(o, ending));

        phases().next();
    }

    @Override
    public void delete() {
        getState().mustNotBe(DELETING, DELETED);

        changeStateTo(DELETING, LGGameDeletingEvent::new);

        players.values().forEach(this::leave);
        terminableRegistry.closeAndReportException();

        changeStateTo(DELETED, LGGameDeletedEvent::new);
    }

    @Override
    public void nextTimeOfDay() {
        getState().mustBe(STARTED);

        if (turn.getTime() == LGGameTurnTime.DAY) {
            turn.setTurnNumber(turn.getTurnNumber() + 1);
            turn.setTime(LGGameTurnTime.NIGHT);
        } else {
            turn.setTime(LGGameTurnTime.DAY);
        }

        Events.call(new LGTurnChangeEvent(this));
    }

    @Override
    public <T> T resolve(Class<T> clazz) {
        try (OrchestratorScope.Block ignored = scope.use(this)) {
            return injector.getInstance(clazz);
        }
    }

    @Override
    public <T> T resolve(Provider<T> provider) {
        try (OrchestratorScope.Block ignored = scope.use(this)) {
            return provider.get();
        }
    }

    private boolean deleteIfEmpty() {
        if (isEmpty() && getState().isEnabled()) {
            delete();
            return true;
        }
        return false;
    }

    private void updateLobbyStuff() {
        this.getState().mustBe(UNINITIALIZED, WAITING_FOR_PLAYERS, READY_TO_START);

        validateComposition();
        if (isFull() && isCompositionValid()) {
            changeStateTo(READY_TO_START, LGGameReadyToStartEvent::new);
        } else {
            changeStateTo(WAITING_FOR_PLAYERS, LGGameWaitingForPlayersEvent::new);
        }
    }

    private void checkPlayer(Player player) throws PlayerJoinException {
        // The LGGameManager approach works well for now
        // but it will cause issues with BungeeCord support.

        if (!player.isOnline()) {
            throw new PlayerOfflineException(player);
        }

        String permission = "loupsgarous.game.join";
        if (!player.hasPermission(permission)) {
            throw new PermissionMissingException(permission, player);
        }

        if (!allowsJoin()) {
            throw InaccessibleLobbyException.lobbyLocked();
        } else if (getSlotsTaken() == getTotalSlotCount()) {
            throw InaccessibleLobbyException.lobbyFull();
        }

        if (gameManager.getPlayerInGame(player).isPresent()) {
            throw new PlayerAlreadyInGameException(player);
        }
    }

    @Override
    public LGPlayer join(Player player) throws PlayerJoinException {
        checkPlayer(player);

        OrchestratedLGPlayer lgPlayer = new OrchestratedLGPlayer(player.getUniqueId(), this);
        players.put(lgPlayer.getPlayerUUID(), lgPlayer);

        if (state != UNINITIALIZED) {
            updateLobbyStuff();
        }

        executeAfterInitialization(() -> {
            lobbyTeleporter.teleportPlayerIn(player);
            Events.call(new LGPlayerJoinEvent(this, player, lgPlayer));

            chat().sendToEveryone(player(player.getName()) + lobbyMessage(" a rejoint la partie ! ") +
                                  slots(getSlotsDisplay()));
        });

        return lgPlayer;
    }

    @Override
    public boolean leave(UUID playerUUID) {
        LGPlayer player = players.get(playerUUID);
        if (player == null || player.isAway()) return false;

        ((OrchestratedLGPlayer) player).goAway();
        if (allowsJoin()) {
            players.remove(playerUUID);
        }

        player.minecraftNoContext(lobbyTeleporter::teleportPlayerOut);

        if (isGameRunning() && player.isAlive()) {
            player.die(PlayerQuitKillCause.INSTANCE);
        }
        if (allowsJoin()) {
            chat().sendToEveryone(player(player.getName()) + lobbyMessage(" a quitté la partie ! ") +
                                  slots(getSlotsDisplay()));
        }

        Events.call(new LGPlayerQuitEvent(this, playerUUID, player));

        // Are they all gone?
        if (!deleteIfEmpty() && allowsJoin()) {
            updateLobbyStuff();
        }

        return true;
    }

    @Override
    public World getWorld() {
        return lobbyTeleporter.getWorld();
    }

    @Override
    public boolean allowsJoin() {
        return this.getState() == LGGameState.UNINITIALIZED ||
               this.getState() == LGGameState.WAITING_FOR_PLAYERS ||
               this.getState() == LGGameState.READY_TO_START;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public GameBundle getGameBundle() {
        return gameBundle.get();
    }

    @Override
    public Observable<GameBundle> observeGameBundle() {
        return gameBundle.observe();
    }

    private GameBundle createBundle(ModBundle modBundle) {
        return new GameBundle(modBundle.createExtensions(), this::resolve);
    }

    private void updateGameBundle() {
        long startTime = System.nanoTime();

        GameBundle oldBundle = gameBundle.get();
        GameBundle newBundle = createBundle(modBundle.get());

        gameBundle.set(newBundle);
        if (oldBundle != null && allowsJoin()) {
            removeBundleRemovedCards(oldBundle, newBundle);
        }

        long elapsed = System.nanoTime() - startTime;
        logger.info("GameBundle update took " + TimeUnit.NANOSECONDS.toMicros(elapsed) + "µs");
    }

    @Override
    public ImmutableComposition getComposition() {
        return composition;
    }

    @Override
    public void setComposition(Composition composition) {
        Preconditions.checkArgument(allowsJoin(),
                "Impossible to change the composition while the game is not in is lobby phase.");

        HashMultiset<LGCard> cards = HashMultiset.create(composition.getContents());

        // Add some cards if there are not enough cards for the players we have.
        while (cards.size() < getSlotsTaken()) {
            // TODO: What happens if VillageoisCard is not in the bundle? Hmmm?
            cards.add(VillageoisCard.INSTANCE);
        }

        this.composition = new ImmutableComposition(cards);

        if (state != UNINITIALIZED) {
            updateLobbyStuff();
            Events.call(new LGLobbyCompositionUpdateEvent(this));
        }
    }

    @Override
    public @Nullable CompositionValidator.Problem.Type getWorstCompositionProblemType() {
        return worseCompositionProblemType;
    }

    @Override
    public boolean isCompositionValid() {
        return worseCompositionProblemType != CompositionValidator.Problem.Type.IMPOSSIBLE;
    }

    private void removeBundleRemovedCards(GameBundle oldBundle, GameBundle newBundle) {
        Sets.SetView<LGCard> removedCards =
                Sets.difference(oldBundle.contents(CARDS), newBundle.contents(CARDS));

        if (removedCards.isEmpty()) {
            return;
        }

        ImmutableComposition newComposition = composition.with(cards -> {
            for (LGCard removedCard : removedCards) {
                cards.remove(removedCard, Integer.MAX_VALUE);
            }
        });

        setComposition(newComposition);
    }

    private void validateComposition() {
        if (!allowsJoin()) {
            return;
        }

        worseCompositionProblemType =
                getGameBundle().handler(COMPOSITION_VALIDATORS).validate(composition).stream()
                        .map(CompositionValidator.Problem::getType)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
    }

    @Override
    public <T extends OrchestratorComponent> T component(MetadataKey<T> key) {
        checkDelayedDependencies();

        return delayedDependencies.componentMap.get(key).orElseThrow(NoSuchElementException::new);
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public LGGameState getState() {
        return state;
    }

    @Override
    public ImmutableSet<LGPlayer> getPlayers() {
        return ImmutableSet.copyOf(players.values());
    }

    @Override
    public LGGameTurn getTurn() {
        return turn;
    }

    @Override
    @Nullable
    public LGEnding getEnding() {
        return ending;
    }

    @Override
    @Nullable
    public LGPlayer getOwner() {
        return owner;
    }

    @Override
    public void setOwner(LGPlayer owner) {
        if (this.owner == owner) return;
        this.owner = owner;

        Events.call(new LGLobbyOwnerChangeEvent(this, owner));
    }

    @Override
    public ModBundle getModBundle() {
        return modBundle.get();
    }

    @Override
    public void setModBundle(ModBundle modBundle) {
        if (!allowsJoin()) {
            throw new IllegalStateException("The game is locked.");
        }
        this.modBundle.set(modBundle);
        updateGameBundle();
    }

    @Override
    public Observable<ModBundle> observeModBundle() {
        return modBundle.observe();
    }

    @Override
    public MetadataMap getMetadata() {
        return metadataMap;
    }

    @Override
    public Optional<LGPlayer> getPlayer(UUID playerUUID) {
        return Optional.ofNullable(players.get(playerUUID));
    }

    @Override
    public LGPlayer getPlayerOrThrow(UUID playerUUID) {
        LGPlayer player = players.get(playerUUID);
        if (player == null) {
            throw new PlayerAbsentException(
                    "The given player UUID " + playerUUID +
                    " is not present in game " + this);
        }
        return player;
    }

    @Override
    public LGPlayer ensurePresent(LGPlayer player) {
        if (!players.containsValue(player)) {
            throw new PlayerAbsentException(
                    "The given player " + player + " is not present in game " + this);
        }
        return player;
    }

    private void registerEventListeners() {
        Events.merge(PlayerEvent.class, PlayerQuitEvent.class, PlayerKickEvent.class)
                .handler(e -> leave(e.getPlayer()))
                .bindWith(this);

        Events.subscribe(PlayerChangedWorldEvent.class)
                .filter(e -> e.getFrom() == getWorld())
                .handler(e -> leave(e.getPlayer()))
                .bindWith(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("state", state)
                .toString();
    }

    @Nonnull
    @Override
    public <T extends AutoCloseable> T bind(@Nonnull T terminable) {
        return terminableRegistry.bind(terminable);
    }

    private void changeStateTo(LGGameState state,
                               Function<? super LGGameOrchestrator, ? extends LGEvent> eventFunction) {
        if (this.getState() == state) return;

        LGGameState oldState = this.getState();
        this.state = state;

        logger.fine("State changed: " + oldState + " -> " + state);

        Events.call(eventFunction.apply(this));
    }

    private void checkDelayedDependencies() {
        Preconditions.checkState(delayedDependencies != null,
                "This game is not initialized yet.");
    }

    private void executeAfterInitialization(Runnable action) {
        if (getState() == UNINITIALIZED) {
            postInitializationActions.add(action);
        } else {
            action.run();
        }
    }

    private static final class DelayedDependencies implements Terminable {
        final MetadataMap componentMap = MetadataMap.create();

        @Inject
        DelayedDependencies(Map<MetadataKey<?>, OrchestratorComponent> rawComponentMap) {
            rawComponentMap.forEach(this::addToComponentMap);
        }

        @SuppressWarnings("unchecked")
        private <T extends OrchestratorComponent>
        void addToComponentMap(MetadataKey<?> key, T entry) {
            this.componentMap.put((MetadataKey<? super T>) key, entry);
        }

        @Override
        public void close() throws CompositeClosingException {
            CompositeTerminable terminables = CompositeTerminable.create();

            for (Object value : componentMap.asMap().values()) {
                terminables.bind(((OrchestratorComponent) value));
            }

            terminables.close();
        }
    }

    private class OrchestratorLogger extends Logger {
        private final String prefix;

        public OrchestratorLogger() {
            super(MinecraftLGGameOrchestrator.this.getClass().getCanonicalName(), null);
            prefix = "[LoupsGarous] (Game " + id + ") ";
            setParent(getPlugin().getLogger());
            setLevel(Level.ALL);
        }

        @Override
        public void log(LogRecord record) {
            record.setMessage(prefix + record.getMessage());
            super.log(record);
        }
    }
}
