/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */
package com.sikulix.util;

/**
 * INTERNAL USE
 */
public interface EventSubject {

  public void addObserver(EventObserver o);

  public void notifyObserver();
}
