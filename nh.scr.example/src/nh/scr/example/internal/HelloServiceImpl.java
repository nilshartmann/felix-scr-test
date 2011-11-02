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


import nh.scr.example.HelloService;
import nh.scr.example.TranslationService;

/**
 * @author Nils Hartmann (nils@nilshartmann.net)
 * 
 */
public class HelloServiceImpl implements HelloService {
  
  private TranslationService _translationService;
  
  protected void activate() {
    System.out.println("Activated!");
  }

  /*
   * (non-Javadoc)
   * 
   * @see nh.scr.example.HelloService#sayHello(java.lang.String)
   */
  @Override
  public String sayHello(String name) {
    return "Hello, " + name;
  }

  /**
   * @param translationService the translationService to set
   */
  public void setTranslationService(TranslationService translationService) {
    System.out.printf("Set TransationService: %s%n", translationService);
    _translationService = translationService;
  }
  
  

}
