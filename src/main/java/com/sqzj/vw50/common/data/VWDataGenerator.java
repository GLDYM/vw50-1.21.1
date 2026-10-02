package com.sqzj.vw50.common.data;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.common.data.provider.VWLanguageProvider;
import com.sqzj.vw50.common.data.provider.VWRecipeProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = VW50.MOD_ID)
public class VWDataGenerator {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        event.createProvider(VWRecipeProvider::new);
        event.createProvider((output, lookup) -> new VWLanguageProvider(output, "en_us"));
        event.createProvider((output, lookup) -> new VWLanguageProvider(output, "zh_cn"));
    }

}
