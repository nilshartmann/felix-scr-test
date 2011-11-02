/*******************************************************************************
 * Copyright (c) 2011 Nils Hartmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nils Hartmann - initial API and implementation
 ******************************************************************************/
package nh.scr.example.internal;

import nh.scr.example.TranslationService;

/**
 * @author Nils Hartmann (nils@nilshartmann.net)
 *
 */
public class TranslationServiceImpl implements TranslationService {

  /* (non-Javadoc)
   * @see nh.scr.example.TranslationService#getTranslatedString(java.lang.String)
   */
  @Override
  public String getTranslatedString(String key) {
    return key.toUpperCase();
  }
  
  protected void activate() {
    System.out.println("TranslationServiceImpl activate()");
  }

}
