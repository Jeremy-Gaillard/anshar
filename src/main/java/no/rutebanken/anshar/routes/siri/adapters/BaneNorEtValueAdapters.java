package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.processor.BaneNorIdPlatformPostProcessor;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.SubscriptionSetup;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="banenoret")
public class BaneNorEtValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(SubscriptionSetup subscriptionSetup) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();
        valueAdapters.add(new BaneNorIdPlatformPostProcessor());

//        valueAdapters.add(new OperatorFilterPostProcessor(Arrays.asList("BN", "GR", "TÅB")));

        return valueAdapters;
    }
}
