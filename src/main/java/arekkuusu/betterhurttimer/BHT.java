package arekkuusu.betterhurttimer;

import arekkuusu.betterhurttimer.api.BHTAPI;
import arekkuusu.betterhurttimer.api.capability.HealthProvider;
import arekkuusu.betterhurttimer.api.capability.HurtProvider;
import arekkuusu.betterhurttimer.api.capability.data.HurtSourceInfo;
import arekkuusu.betterhurttimer.client.ClientProxy;
import arekkuusu.betterhurttimer.common.ServerProxy;
import arekkuusu.betterhurttimer.common.command.CommandExport;
import arekkuusu.betterhurttimer.common.proxy.IProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(BHT.MOD_ID)
public final class BHT {

    //Useful names
    public static final String MOD_ID = "betterhurttimer";
    public static final String MOD_NAME = "Better Hurt Timer";

    private static IProxy proxy;
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    public static IProxy getProxy() {
        return proxy;
    }

    public BHT() {
        proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BHTConfig.Holder.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BHTConfig.Holder.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        //MinecraftForge.EVENT_BUS.register(new HurtCapability.Handler());
        //MinecraftForge.EVENT_BUS.register(new HurtProvider());
        //MinecraftForge.EVENT_BUS.register(new HealthCapability.Handler());
        //MinecraftForge.EVENT_BUS.register(new HealthProvider());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onModConfigEvent);
    }

    public void registerCommands(RegisterCommandsEvent event) {
        CommandExport.register(event.getDispatcher());
    }

    public void onModConfigEvent(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() == BHTConfig.Holder.CLIENT_SPEC) {
            BHTConfig.Setup.client(config);
            LOG.debug("Baked client config");
        } else if (config.getSpec() == BHTConfig.Holder.COMMON_SPEC) {
            BHTConfig.Setup.server(config);
            this.initAttackFrames();
            this.initDamageFrames();
            LOG.debug("Baked server config");
        }
    }

    public void initAttackFrames() {
        BHTAPI.ATTACK_THRESHOLD_MAP.clear();
        String patternAttackFrames = "^(.*:.*):((\\d*\\.)?\\d+)$";
        Pattern r = Pattern.compile(patternAttackFrames);
        for (String s : BHTConfig.Runtime.AttackFrames.attackThreshold) {
            Matcher m = r.matcher(s);
            if (m.matches()) {
                BHTAPI.addAttacker(new ResourceLocation(m.group(1)), Double.parseDouble(m.group(2)));
            } else {
                BHT.LOG.warn("[Attack Frames Config] - String " + s + " is not a valid format");
            }
        }
    }

    public void initDamageFrames() {
        BHTAPI.DAMAGE_SOURCE_INFO_MAP.clear();
        String patternAttackFrames = "^(.*):(true|false):?(\\d*)";
        Pattern r = Pattern.compile(patternAttackFrames);
        for (String s : BHTConfig.Runtime.DamageFrames.damageSource) {
            Matcher m = r.matcher(s);
            if (m.matches()) {
                BHTAPI.addSource(new HurtSourceInfo(m.group(1), Boolean.parseBoolean(m.group(2)), Integer.parseInt(m.group(3))));
            } else {
                BHT.LOG.warn("[Damage Frames Config] - String " + s + " is not a valid format");
            }
        }
    }
}