/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */

package com.sikulix.guide;

public interface Transition {

   public interface TransitionListener {
      void transitionOccurred(Object source);
   }

   String waitForTransition(TransitionListener token);

}
