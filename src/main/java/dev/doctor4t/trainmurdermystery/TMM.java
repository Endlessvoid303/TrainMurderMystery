package dev.doctor4t.trainmurdermystery;

import com.google.common.reflect.Reflection;
import dev.doctor4t.trainmurdermystery.block.DoorPartBlock;
import dev.doctor4t.trainmurdermystery.command.*;
import dev.doctor4t.trainmurdermystery.command.argument.TMMGameModeArgumentType;
import dev.doctor4t.trainmurdermystery.command.argument.TimeOfDayArgumentType;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.index.*;
import dev.doctor4t.trainmurdermystery.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMM implements ModInitializer {
    public static final String MOD_ID = "trainmurdermystery";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static @NotNull Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        // Init constants
        GameConstants.init();

        // Registry initializers
        Reflection.initialize(TMMDataComponentTypes.class);
        TMMSounds.initialize();
        TMMEntities.initialize();
        TMMBlocks.initialize();
        TMMItems.initialize();
        TMMBlockEntities.initialize();
        TMMParticles.initialize();

        // Register command argument types
        ArgumentTypeRegistry.registerArgumentType(id("timeofday"), TimeOfDayArgumentType.class, ConstantArgumentSerializer.of(TimeOfDayArgumentType::timeofday));
        ArgumentTypeRegistry.registerArgumentType(id("gamemode"), TMMGameModeArgumentType.class, ConstantArgumentSerializer.of(TMMGameModeArgumentType::gamemode));

        // Register commands
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            GiveRoomKeyCommand.register(dispatcher);
            SetTrainSpeedCommand.register(dispatcher);
            StartCommand.register(dispatcher);
            StopCommand.register(dispatcher);
            ResetWeightsCommand.register(dispatcher);
            SetVisualCommand.register(dispatcher);
            ForceRoleCommand.register(dispatcher);
        }));

        PayloadTypeRegistry.playS2C().register(ShootMuzzleS2CPayload.ID, ShootMuzzleS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PoisonUtils.PoisonOverlayPayload.ID, PoisonUtils.PoisonOverlayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunDropPayload.ID, GunDropPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TaskCompletePayload.ID, TaskCompletePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnnounceWelcomePayload.ID, AnnounceWelcomePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnnounceEndingPayload.ID, AnnounceEndingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KnifeStabPayload.ID, KnifeStabPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GunShootPayload.ID, GunShootPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StoreBuyPayload.ID, StoreBuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NoteEditPayload.ID, NoteEditPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KnifeStabPayload.ID, new KnifeStabPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(GunShootPayload.ID, new GunShootPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(StoreBuyPayload.ID, new StoreBuyPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(NoteEditPayload.ID, new NoteEditPayload.Receiver());

        Scheduler.init();
    }

    public static boolean isSkyVisibleAdjacent(@NotNull Entity player) {
        var mutable = new BlockPos.Mutable();
        var playerPos = BlockPos.ofFloored(player.getEyePos());
        for (var x = -1; x <= 1; x += 2) {
            for (var z = -1; z <= 1; z += 2) {
                mutable.set(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);
                if (player.getWorld().isSkyVisible(mutable)) {
                    return !(player.getWorld().getBlockState(playerPos).getBlock() instanceof DoorPartBlock);
                }
            }
        }
        return false;
    }

    public static boolean isExposedToWind(@NotNull Entity player) {
        var mutable = new BlockPos.Mutable();
        var playerPos = BlockPos.ofFloored(player.getEyePos());
        for (var x = 0; x <= 10; x++) {
            mutable.set(playerPos.getX() - x, player.getEyePos().getY(), playerPos.getZ());
            if (!player.getWorld().isSkyVisible(mutable)) {
                return false;
            }
        }
        return true;
    }
}

// Fixing the mood system
// 3 issues: Passive instead of active, debuff is both annoying and externally noticeable
// done: Rearrange the train cars to prevent all POIs being separated by all the sleeping cars
// done: Better tasks: mood goes down gradually, completing tasks is a single action to bring it back up
//  (new task system is more meant to make players vulnerable to the killer in a different way from splitting them up)
// done: - Get a snack from restaurant task (food platter block + food items)
// done: - Get a drink from the bar task (drink tray block + custom drink items)
// done: - Sleeping task requiring you to sleep for 8s
// done: - Get some fresh air reduced to going walking outside for 8s
// done: - Change mood down effect from speed to: (to prevent players being able to innocent each other on an easily observable change)
// done:    - Mid mood: Imagining random items in other player's hands (changes every so often) + a little more shake
// done:    - Low mood: Dead bodies disappear after 10s + even more shake
// done: Mood system shows up for the killer as well, custom mood icon indicating no effect, but here to suggest how to play along and fake tasks
// done: Killer can see mood of players with instinct to gaslight them

// Fixing the detective
// done: Remove revolver bullet count but make detectives drop the gun on innocent kill
//              to prevent detectives gunning down people and giving more weight to the choice as well as offer a chance to other players to make decisions
//              also sets mood to 0 as extra punishment
//              Cannot be picked up by: The person who shot wrongly or killers, if you already have a revolver
//              but then can't people force the killer to try and pick up the gun to see if they are one? No I'll explain why later
// done: Make the detective drop the gun on killed (that the killer cannot pick up, to prevent soft locking)
// done: New name display system to allow anyone to know player's names + revolver indicator if you are going to hit your target
// done: Also removed cooldown in creative LMAO
// done: Remove body bags so make player corpses turn into skeletons after some time (since the detective role is no longer really a role and depends on who carries the gun, it's hard to keep the body bag item)

// Fixing the killer
// done: Remove target system and make the win condition killing all civilians, essentially ditching the whole hitman vibe for a simple killer
// done: Instinct now shows other killers instead of targets allowing them to scheme together + info like dropped guns to keep players away
// done: You'll notice the killer doesn't start with the knife or the lockpick and that we have an additional coin UI element in the top left, because ITEM SHOP
//              Idea is we're gonna give the killer a bunch of tools and you are free to play how you want! And there's gonna be a lot of options:
// done: - Fixing the knife
// done:        Now has a kill indicator so you know for sure when you get a kill
// done:        More knockback for easier push kills (show clip of player shooting me when being pushed off)
// done: - Revolver
//             allows the killer to potentially pass as a detective / passenger with a gun on top of giving a ranged option
//             double edged sword because just like for other passengers, you drop it when you shoot an innocent, which means that there is now one more gun in circulation people can use against you
//             counter to people trying to make you pick up the gun and confirm you are the killer because remember you can't pick it up if you already have a revolver
//              so the killer can buy it and bluff that they already have one and use it as a justification
// done: - Grenade for clumped up people (foils the grouping up cheese)
//              Detail of the thrown grenade not having the pin or the handle
// done: - Psycho mode (wanted to have an anonymous killer originally for the horror element, this also allows the killer to go crazy how some wanted to)
//              Ambience can be heard by all players, so you know when to run. Also gives a use to rooms as you can hide in them
//              Psycho mode can shrug off one bullet, cause too easy to counter otherwise
// done: - Poison (poisons the next food or drink item)
// done: - Scorpion (poisons the next person sleeping in the bed)
// done: - Firecracker (luring people, shooting the gun in spectator often led to people rushing in from curiosity, allowing the killer to manipulate players)
// done: - Crowbar (perma opening a door should be a killer ability, allows for creative kills where you can push off players from train doors, as well as allowing passengers to use the exterior in order to give plausible deniability to killers using it to relocate)
// done: - Body bag purchasable for the killer (can be used to clean up a kill, but very expensive)
// done: - Blackout item + true darkness (increases the horror aspect + amazing scenario of lights turning off and someone being dead when they turn back on + blackout and psycho mode)
//              True darkness doesn't work that well because of skylight seeping into the block, add a light barrier block that blocks it from entering
//              Disable name renderer for the duration of the blackout to prevent people seeing in the dark
//              Nightvis with instinct for killers
// done: - Note (allows the killer to leave messages, fun for encouraging the roleplay aspect)
// done: 5 coins every 10 seconds + 100 on civilian kill
// done: Timer for killer

// Polish
// TORECORD: Game start and end messages
// TORECORD: Player collisions for awkward corridor interactions and other shenanigans like stacking to get on top of the train
// TORECORD: Train chimney smoke
// TORECORD: Player counter in lobby
// TORECORD: Ringable horn that can be used as a meeting callout as everyone on the map can hear it
// TORECORD: Initial discovery mode
// TORECORD: Final game mode, for fun: loose ends



// POST RECORDING
// TORECORD: Ability to customize time of day for supporters + snow density
// TODO: Watermark?
// TODO: Sticky note price cheaper
// TODO: note reset button on the screen to remove all typed text
// TODO: Have knife be red on full charge only
