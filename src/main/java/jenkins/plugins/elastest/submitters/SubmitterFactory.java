/*
 * The MIT License
 *
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.elastest.submitters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import jenkins.plugins.elastest.submitters.ElasTestSubmitter.SubmitterType;

/**
 * Factory for AbstractElasTestSubmitter objects.
 *
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public final class SubmitterFactory {
    private static AbstractElasTestSubmitter instance = null;

    private static final Map<SubmitterType, Class<?>> INDEXER_MAP;
    static {
        Map<SubmitterType, Class<?>> indexerMap = new HashMap<SubmitterType, Class<?>>();

        indexerMap.put(SubmitterType.LOGSTASH, LogstashSubmitter.class);

        INDEXER_MAP = Collections.unmodifiableMap(indexerMap);
    }

    public static synchronized ElasTestSubmitter getInstance(SubmitterType type,
            String host, Integer port, String key, String username,
            String password) throws InstantiationException {
        if (type == null || !INDEXER_MAP.containsKey(type)) {
            throw new InstantiationException(
                    "[elastest-plugin]: Unknown IndexerType '" + type
                            + "'. Did you forget to configure the plugin?");
        }

        port = (port == null ? Integer.valueOf(-1) : port);

        if (shouldRefreshInstance(type, host, port, key, username, password)) {
            try {
                Class<?> indexerClass = INDEXER_MAP.get(type);
                Constructor<?> constructor = indexerClass.getConstructor(
                        String.class, int.class, String.class, String.class,
                        String.class);
                instance = (AbstractElasTestSubmitter) constructor
                        .newInstance(host, port, key, username, password);
            } catch (NoSuchMethodException e) {
                throw new InstantiationException(
                        ExceptionUtils.getRootCauseMessage(e));
            } catch (InvocationTargetException e) {
                throw new InstantiationException(
                        ExceptionUtils.getRootCauseMessage(e));
            } catch (IllegalAccessException e) {
                throw new InstantiationException(
                        ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return instance;
    }

    private static boolean shouldRefreshInstance(SubmitterType type, String host,
            int port, String key, String username, String password) {
        if (instance == null) {
            return true;
        }

        boolean matches = (instance.getSubmitterType() == type)
                && StringUtils.equals(instance.host, host)
                && (instance.port == port)
                && StringUtils.equals(instance.key, key)
                && StringUtils.equals(instance.username, username)
                && StringUtils.equals(instance.password, password);
        return !matches;
    }
}
