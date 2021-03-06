/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.strimzi.controller.topic;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LabelPredicateTest {

    @Test
    public void testCtorError() {
        try {
            new LabelPredicate("foo");
            fail();
        } catch(IllegalArgumentException e) {

        }

        try {
            new LabelPredicate("foo", "1", "bar");
            fail();
        } catch(IllegalArgumentException e) {

        }
    }

    /**
     * See https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#syntax-and-character-set
     * and https://github.com/kubernetes/community/blob/master/contributors/design-proposals/architecture/identifiers.md
     */
    @Test
    public void testParser() {
        asserParseFails("foo");

        asserParseFails("foo=bar,");

        // Disallow these for now
        asserParseFails("foo==bar");
        asserParseFails("foo!=bar");
        asserParseFails("foo=bar,");
        asserParseFails("foo=bar,,");
        asserParseFails(",foo=bar");
        asserParseFails(",,foo=bar");
        asserParseFails("8/foo=bar");
        asserParseFails("//foo=bar");
        asserParseFails("a/b/foo=bar");
        asserParseFails("foo=bar/");

        // label values can be empty
        LabelPredicate.fromString("foo=");

        // single selector
        LabelPredicate.fromString("foo=bar");
        LabelPredicate.fromString("foo/bar=baz");
        LabelPredicate.fromString("foo.io/bar=baz");

        // multiple selectors
        LabelPredicate.fromString("foo=bar,name=gee");
        LabelPredicate.fromString("foo=bar,name=");
        LabelPredicate.fromString("foo=bar,lala/name=");
    }

    private void asserParseFails(String foo) {
        try {
            LabelPredicate.fromString(foo);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testLabels() {
        LabelPredicate p = new LabelPredicate("foo", "1", "bar", "2");
        Map<String, String> m = new HashMap<>(2);
        m.put("foo", "1");
        m.put("bar", "2");
        assertEquals(m, p.labels());
    }

    @Test
    public void testTest() {
        LabelPredicate p = new LabelPredicate("foo", "1", "bar", "2");

        HasMetadata h = new ConfigMapBuilder().editOrNewMetadata()
                .addToLabels("foo", "1").addToLabels("bar", "2")
                .endMetadata().build();
        assertTrue(p.test(h));

        h = new ConfigMapBuilder().editOrNewMetadata()
                .addToLabels("foo", "1").addToLabels("bar", "2").addToLabels("baz", "3")
                .endMetadata().build();
        assertTrue(p.test(h));

        h = new ConfigMapBuilder().editOrNewMetadata()
                .addToLabels("foo", "2").addToLabels("bar", "2")
                .endMetadata().build();
        assertFalse(p.test(h));

        h = new ConfigMapBuilder().editOrNewMetadata()
                .addToLabels("foo", "1")
                .endMetadata().build();
        assertFalse(p.test(h));

        h = new ConfigMapBuilder().editOrNewMetadata()
                .endMetadata().build();
        assertFalse(p.test(h));
    }
}
