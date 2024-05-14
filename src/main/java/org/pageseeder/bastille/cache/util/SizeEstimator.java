/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.bastille.cache.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * A simple class to estimate the size of a cache based on previously calculated sized
 * and the number of elements.
 *
 * @author Christophe Lauret
 * @version Bastille 0.8.3
 */
public final class SizeEstimator {

  /** Singleton instance */
  private static final SizeEstimator SINGLETON = new SizeEstimator();

  /**
   * A new sample is taken when the number of elements in the cache exceeds the number of elements in the sample
   * times this factor.
   */
  private static final float RESAMPLE_FACTOR = 1.5f;

  /**
   * Samples for "in memory" sizes mapped to the cache names.
   */
  private final Map<String, Sample> inMemorySamples = new ConcurrentHashMap<>();

  /**
   * Samples for "in memory" sizes mapped to the cache names.
   */
  private final Map<String, Sample> onDiskSamples = new ConcurrentHashMap<>();

  /**
   * Request singleton instead.
   */
  private SizeEstimator() {
  }

  /**
   * @return the single instance.
   */
  public static SizeEstimator singleton() {
    return SINGLETON;
  }

  /**
   * Check whether a new "in memory" size sample need to be re-calculated.
   *
   * @param cache The cache.
   * @return <code>true</code> if a new sample was recalculated;
   *         <code>false</code> otherwise.
   */
  public boolean checkInMemorySample(Ehcache cache) {
    Sample sample = this.inMemorySamples.get(cache.getName());
    int elements = cache.getSize();
    if (sample == null || sample.elements() * RESAMPLE_FACTOR < elements || sample._bytesize == 0) {
      long bytesize = elements > 0? calculateInMemorySize(cache) : 0;
      sample = new Sample(elements, bytesize);
      this.inMemorySamples.put(cache.getName(), sample);
      return elements > 0;
    }
    return false;
  }
  
    
  public long calculateInMemorySize(Ehcache cache) {
	    long size = 0;
	    IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
	    List keys = cache.getKeys();
	    int depth = 0; // Initialize depth

	    for (Object key : keys) {
	        Element element = cache.get(key);
	        if (element != null) {
	            Object value = element.getObjectValue();
	            // Increment depth for each recursive call
	            size += estimateSizeOfCache(value, visited, depth + 1);
	        }
	    }

	    return size;
	}
  
  
  
	/**
	 * Estimates the size of an object in memory, including objects reachable from it, up to a certain depth.
	 * This method recursively traverses objects, collections, maps, and arrays to estimate their total memory footprint.
	 * It uses a set of visited objects to avoid infinite recursion and limits the depth of traversal to prevent stack overflow.
	 * 
	 * @param object The object to estimate the size of. Can be any Java object, collection, map, or array.
	 * @param visited A set of system identity hash codes of already visited objects to avoid revisiting them.
	 * @param depth The current depth of the recursion. The method stops recursion when depth exceeds 20.
	 * @return The estimated size of the object in bytes. This is a rough estimate and not an exact measurement.
	 */
  private long estimateSizeOfCache(Object object, IdentityHashMap<Object, Boolean> visited, int depth) {
	  if (object == null || visited.containsKey(object) || depth > 20) {
		  return 0;
	  }
	  visited.put(object, Boolean.TRUE);
	  long size = 0;
	  Class<?> objClass = object.getClass();

	  // Accurately estimate size for primitive types
	  if (objClass.isPrimitive()) {
		  size = getSizeOfPrimitiveType(objClass);
	  } else if (object instanceof String) {
		  size = ((String) object).length() * Character.BYTES;
	  } else if (object instanceof Collection) {
		  Collection<?> collection = (Collection<?>) object;
		  for (Object item : collection) {
			  size += estimateSizeOfCache(item, visited, depth + 1);
		  }
	  } else if (object instanceof Map) {
		  Map<?, ?> map = (Map<?, ?>) object;
		  for (Map.Entry<?, ?> entry : map.entrySet()) {
			  size += estimateSizeOfCache(entry.getKey(), visited, depth + 1) + estimateSizeOfCache(entry.getValue(), visited, depth + 1);
		  }
	  } else if (objClass.isArray()) {
		  int length = Array.getLength(object);
		  for (int i = 0; i < length; i++) {
			  size += estimateSizeOfCache(Array.get(object, i), visited, depth + 1);
		  }
	  } else {
		  // Estimate size for unknown types by inspecting their fields
		  while (objClass != null && objClass != Object.class) {
			  Field[] fields = objClass.getDeclaredFields();
			  for (Field field : fields) {
				  if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
					  boolean accessible = field.isAccessible();
					  try {
						  field.setAccessible(true);
						  Object fieldValue = field.get(object);
						  size += estimateSizeOfCache(fieldValue, visited, depth + 1);
					  } catch (IllegalAccessException e) {

						  size += 50;// Assume a default size for inaccessible fields
					  }catch (InaccessibleObjectException e) {

						  size += 50; // Assume a default size for inaccessible fields

					  } finally {
						  field.setAccessible(accessible);
					  }
				  }
			  }
			  objClass = objClass.getSuperclass();
		  }
	  }
	  return size;
  }

	/**
	 * Returns the size of a primitive type.
	 * @param type The class representing the primitive type.
	 * @return The size in bytes of the primitive type.
	 */
	private long getSizeOfPrimitiveType(Class<?> type) {
	    if (type == boolean.class) return 1;
	    if (type == byte.class) return 1;
	    if (type == char.class) return Character.BYTES;
	    if (type == short.class) return Short.BYTES;
	    if (type == int.class) return Integer.BYTES;
	    if (type == float.class) return Float.BYTES;
	    if (type == long.class) return Long.BYTES;
	    if (type == double.class) return Double.BYTES;
	    return 0; // void and other non-primitive types
	}

  /**
   * Check whether a new "on disk" size sample need to be re-calculated.
   *
   * @param cache The cache.
   * @return <code>true</code> if a new sample was recalculated;
   *         <code>false</code> otherwise.
   */
  public boolean checkOnDiskSample(Ehcache cache) {
    Sample sample = this.onDiskSamples.get(cache.getName());
    int elements = cache.getSize();
    if (sample == null || sample.elements()*2 < elements) {
      long bytesize = elements > 0? cache.calculateOnDiskSize() : 0;
      sample = new Sample(elements, bytesize);
      this.onDiskSamples.put(cache.getName(), sample);
      return elements > 0;
    }
    return false;
  }

  /**
   * Returns the "in memory" size from the cache.
   *
   * <p>This method does not indicate whether the size was calculated or estimated from a previous sample.
   *
   * @param cache The cache
   * @return return the actual value or -1;
   */
  public long getInMemorySize(Ehcache cache) {
    synchronized (this.inMemorySamples) {
      checkInMemorySample(cache);
      return estimateInMemorySize(cache.getName(), cache.getSize());
    }
  }

  /**
   * Returns the "on disk" size from the cache.
   *
   * <p>This method does not indicate whether the size was calculated or estimated from a previous sample.
   *
   * @param cache The cache
   * @return return the actual value or -1;
   */
  public long getOnDiskSize(Ehcache cache) {
    synchronized (this.onDiskSamples) {
      checkOnDiskSample(cache);
      return estimateOnDiskSize(cache.getName(), cache.getSize());
    }
  }

  /**
   * Estimate the "in memory" size based on previous samples.
   *
   * @param name The name of the cache
   * @param elements The number of elements
   * @return return the actual value or -1;
   */
  public long estimateInMemorySize(String name, int elements) {
    return estimate(this.inMemorySamples, name, elements);
  }

  /**
   * Estimate the "on disk" size based on previous samples.
   *
   * @param name The name of the cache
   * @param elements The number of elements
   * @return return the actual value or -1;
   */
  public long estimateOnDiskSize(String name, int elements) {
    return estimate(this.onDiskSamples, name, elements);
  }

  /**
   * Returns an estimates from a previous sample.
   *
   * @param samples
   * @param name
   * @param elements
   * @return
   */
  private static long estimate(Map<String, Sample> samples, String name, int elements) {
    Sample sample = samples.get(name);
    long estimate = -1;
    if (sample != null) {
      estimate = sample.estimate(elements);
    }
    return estimate;
  }

  /**
   * A byte size sample.
   *
   * <p>This class is immutable, but new samples can be created,
   *
   * @author Christophe Lauret
   * @version 10 March 2013
   */
  private static class Sample {

    /**
     * Number of elements in the sample.
     */
    private final int _elements;

    /**
     * The size in bytes for that number of elements.
     */
    private final long _bytesize;

    /**
     * @param elements Number of elements
     * @param bytesize The size in bytes
     */
    public Sample(int elements, long bytesize) {
      this._elements = elements;
      this._bytesize = bytesize;
    }

    /**
     * Returns an estimate for the specified number of elements
     *
     * @param elements The number of elements
     * @return the estimated size
     */
    public long estimate(int elements) {
      if (this._elements == elements) return this._bytesize;
      if (this._elements == 0) return 0;
      // XXX: May be larger than MAX_LONG?
      return (this._bytesize * elements) / this._elements;
    }

    /**
     * @return the number of elements in the sample
     */
    public int elements() {
      return this._elements;
    }
  }
}
