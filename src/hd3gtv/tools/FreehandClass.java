/*
 * This file is part of On Road Media Send.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Instance classes and methods without checks the accessibility.
 */
@SuppressWarnings("nls")
public final class FreehandClass {
	
	private Class<?> this_class;
	private Object this_instance;
	
	/**
	 * Class constructor must be public and without params.
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public FreehandClass(String full_class_name) throws ClassNotFoundException, ReflectiveOperationException {
		if (full_class_name == null) {
			throw new NullPointerException("\"full_class_name\" can't to be null");
		}
		this_class = Class.forName(full_class_name);
	}
	
	/**
	 * Used for manually set the object and the class... and force Object signature.
	 */
	public FreehandClass(Object o, Class<?> o_class) {
		this_class = o_class;
		this_instance = o;
	}
	
	private FreehandClass(Object o) {
		this_class = o.getClass();
		this_instance = o;
	}
	
	/**
	 * @return this
	 */
	public FreehandClass setInstance() throws ReflectiveOperationException {
		this_instance = this_class.newInstance();
		return this;
	}
	
	public Object getInstance() {
		return this_instance;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> over_class) {
		return (T) this_instance;
	}
	
	public FreehandClass call(String method, Object... params) throws ReflectiveOperationException {
		Object result = null;
		
		if (params == null) {
			Method m = this_class.getMethod(method);
			result = m.invoke(this_instance);
		} else if (params.length == 0) {
			Method m = this_class.getMethod(method);
			result = m.invoke(this_instance);
		} else {
			Class<?>[] param_type = new Class<?>[params.length];
			Object[] params_objects = new Object[params.length];
			
			for (int pos = 0; pos < params.length; pos++) {
				if (params[pos] != null) {
					Class<?> param_class = params[pos].getClass();
					
					if (param_class.equals(FreehandClass.class)) {
						param_type[pos] = ((FreehandClass) params[pos]).this_class;
						params_objects[pos] = ((FreehandClass) params[pos]).this_instance;
					} else {
						param_type[pos] = param_class;
						params_objects[pos] = params[pos];
					}
				} else {
					param_type[pos] = null;
				}
			}
			
			Method m = this_class.getMethod(method, param_type);
			result = m.invoke(this_instance, params_objects);
		}
		
		if (result != null) {
			return new FreehandClass(result);
		}
		
		return null;
	}
	
	public FreehandClass getVariable(String name) throws ReflectiveOperationException {
		Field field = this_class.getField(name);
		return new FreehandClass(field.get(this_instance));
	}
	
	public static FreehandClass staticCall(String full_class_name, String method, Object... params) throws ReflectiveOperationException {
		FreehandClass fc = new FreehandClass(full_class_name);
		return fc.call(method, params);
	}
	
	public static FreehandClass staticVariable(String full_class_name, String name) throws ReflectiveOperationException {
		FreehandClass fc = new FreehandClass(full_class_name);
		return fc.getVariable(name);
	}
	
}
