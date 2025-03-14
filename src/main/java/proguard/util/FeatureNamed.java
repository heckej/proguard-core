/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.util;

import java.util.Set;

/**
 * Base interface for entities that are part of a feature, as indicated by a
 * feature name.
 * <p/>
 * The entities can for example be classes and resource files, and the feature
 * can be a dynamic feature in an Android app.
 *
 *
 * @author Eric Lafortune
 */
public interface FeatureNamed
{
    /**
     * Returns the feature name for this entity.
     */
    public String getFeatureName();


    /**
     * Sets the feature name for this entity.
     *
     * @param featureName the feature Name to be set.
     */
    public void setFeatureName(String featureName);


    /**
     * Adds the feature name to the set of features this entity is part of.
     *
     * @param featureName the feature name to add.
     */
    public void addExtraFeatureName(String featureName);


    /**
     * Returns the set of feature names for this entity.
     */
    public Set<String> getExtraFeatureNames();


    /**
     * Returns whether the entity is part of the given feature.
     *
     * @param featureName the name of the feature to check.
     */
    public boolean isInFeature(String featureName);
}
