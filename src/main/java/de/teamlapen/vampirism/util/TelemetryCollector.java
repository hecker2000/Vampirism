package de.teamlapen.vampirism.util;

import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.config.VampirismConfig;
import net.minecraft.Util;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.versions.mcp.MCPVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TelemetryCollector {

    public static final Logger LOGGER = LogManager.getLogger();

    public static void execute() {
        // TODO use server telemetry when available
        if (DistExecutor.unsafeRunForDist(() -> () -> true, () -> () -> true) && VampirismConfig.COMMON.collectStats.get()) {
            send();
        }
    }

    private static void send() {
        try {
            HttpClient build = HttpClient.newBuilder().executor(Util.ioPool()).connectTimeout(Duration.ofSeconds(5)).build();
            StringBuilder builder = new StringBuilder();
            builder.append(REFERENCE.SETTINGS_API);
            builder.append("/telemetry/basic");

            Map<String, String> params = new HashMap<>();
            params.put("mod_version", REFERENCE.VERSION.toString());
            params.put("mc_version", MCPVersion.getMCVersion());
            params.put("mod_count", Integer.toString(ModList.get().size()));
            params.put("side", (EffectiveSide.get() == LogicalSide.CLIENT ? "client" : "server"));

            builder.append("?");
            builder.append(params.entrySet().stream().map(s -> s.getKey() + "=" + URLEncoder.encode(s.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&")));

            build.sendAsync(HttpRequest.newBuilder().uri(new URI(builder.toString())).build(), HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to send telemetry data", e);
        }
    }
}
