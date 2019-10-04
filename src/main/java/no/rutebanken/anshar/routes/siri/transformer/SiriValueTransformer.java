/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.rutebanken.anshar.routes.siri.transformer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.rutebanken.anshar.routes.siri.helpers.SiriObjectFactory;
import no.rutebanken.anshar.routes.siri.processor.PostProcessor;
import no.rutebanken.anshar.routes.siri.transformer.impl.OutboundIdAdapter;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SiriValueTransformer {

    public static final String SEPARATOR = "$";

    private static final Logger logger = LoggerFactory.getLogger(SiriValueTransformer.class);

    private static final Map<GetterKey, Set<Method>> cachedGettersForAdapter = new HashMap<>();

    private static final LoadingCache<Class, List<Method>> getterMethodsCache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<Class, List<Method>>() {
                        public List<Method> load(Class clazz) {
                            Method[] methods = clazz.getMethods();
                            List<Method> getterMethods = new ArrayList<>();
                            for (Method method : methods) {
                                if (method.getParameterCount() == 0 &&
                                        !("void".equals(method.getReturnType().getName()))) {
                                    getterMethods.add(method);
                                }
                            }
                            return getterMethods;
                        }
                    });

    private static final List<String> methodsToIgnore = Collections.singletonList("getMonitoringError");

    /**
     *
     * @param xml
     * @param adapters
     * @return
     * @throws JAXBException
     */
    public static Siri parseXml(InputStream xml, List<ValueAdapter> adapters) throws JAXBException, XMLStreamException {
        return transform(SiriXml.parseXml(xml), adapters);
    }

    public static Siri parseXml(InputStream xml) throws JAXBException, XMLStreamException {
        return SiriXml.parseXml(xml);
    }

    public static Siri transform(Siri siri, List<ValueAdapter> adapters) {
        if (siri == null) {
            return null;
        }
        Siri transformed;
        try {
        	transformed = SiriObjectFactory.deepCopy(siri);
        } catch (Exception e) {
            logger.warn("Unable to transform SIRI-object", e);
            return siri;
        }
        if (transformed != null && adapters != null) {

            List<ValueAdapter> valueAdapters = new ArrayList<>();
            for (ValueAdapter adapter : adapters) {
                if (!(adapter instanceof PostProcessor)) {
                    valueAdapters.add(adapter);
                }
            }

            List<PostProcessor> postProcessors = new ArrayList<>();
            for (ValueAdapter valueAdapter : adapters) {
                if ((valueAdapter instanceof PostProcessor)) {
                    postProcessors.add((PostProcessor) valueAdapter);
                }
            }

            for (ValueAdapter a : valueAdapters) {
                try {
                    applyAdapter(transformed, a);
                } catch (Throwable t) {
                    logger.warn("Caught exception while transforming SIRI-object.", t);
                }
            }

            for (PostProcessor processor : postProcessors) {
                try {
                    processor.process(transformed);
                } catch (Throwable t) {
                    logger.warn("Caught exception while post-processing SIRI-object with processor '" + processor + "'", t);
                }
            }
        }

        return transformed;
    }

    /**
     * Recursively applies ValueAdapter to all fields of the specified type within SIRI-packages.
     *
     * Uses getValue()/setValue(...) apply adapters
     *
     * @param obj
     * @param adapter
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private static void applyAdapter(Object obj, ValueAdapter adapter) throws Throwable {

        //Only apply to Siri-classes
        if (obj.getClass().getName().startsWith("uk.org.siri")) {

            GetterKey getterKey = new GetterKey(obj.getClass(), adapter);
            Set<Method> allMethods = cachedGettersForAdapter.get(getterKey);

            if (allMethods == null) {
                allMethods = new HashSet<>();
                allMethods.addAll(getterMethodsCache.get(obj.getClass()));
            }
            for (Method method : allMethods) {
                if (methodsToIgnore.contains(method.getName())) {
                    continue;
                }


                if (method.getReturnType().equals(adapter.getClassToApply())) {

                    Set<Method> methods = cachedGettersForAdapter.getOrDefault(getterKey, new HashSet<>());
                    methods.add(method);
                    cachedGettersForAdapter.put(getterKey, methods);

                    Object previousValue = method.invoke(obj);
                    if (previousValue != null) {
                        String value = (String) previousValue.getClass().getMethod("getValue").invoke(previousValue);
                        String alteredValue;

                        String originalId = value;

                        if (adapter instanceof OutboundIdAdapter) {
                            alteredValue = adapter.apply(value);
                        } else {
                            if (value.contains(SEPARATOR)) {
                                originalId = value.substring(0, value.indexOf(SEPARATOR));
                                alteredValue = adapter.apply(value.substring(value.indexOf(SEPARATOR) + SEPARATOR.length()));
                            } else {
                                alteredValue = adapter.apply(value);
                            }
                            alteredValue = originalId + SEPARATOR + alteredValue;
                        }


                        Method valueSetter = previousValue.getClass().getMethod("setValue", String.class);
                        valueSetter.invoke(previousValue, alteredValue);
                    }
                } else {
                    Object currentValue = method.invoke(obj);
                    if (currentValue != null) {
                        if (currentValue instanceof List) {
                            List list = (List) currentValue;
                            for (Object o : list) {
                                applyAdapter(o, adapter);
                            }
                        } else {
                            applyAdapter(currentValue, adapter);
                        }
                    }
                }
            }
        }
    }

    private static class GetterKey {
        private final ValueAdapter adapter;
        private final Class clazz;

        public GetterKey(Class clazz, ValueAdapter adapter) {
            this.clazz = clazz;
            this.adapter = adapter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GetterKey getterKey = (GetterKey) o;

            if (adapter != null ? !adapter.equals(getterKey.adapter) : getterKey.adapter != null) {
                return false;
            }
            return clazz != null ? clazz.equals(getterKey.clazz) : getterKey.clazz == null;
        }

        @Override
        public int hashCode() {
            int result = adapter != null ? adapter.hashCode() : 0;
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            return result;
        }
    }
}
