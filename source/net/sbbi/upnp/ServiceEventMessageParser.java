/*
 * ============================================================================
 *                 The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 2002 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following  acknowledgment: "This product includes software
 *    developed by SuperBonBon Industries (http://www.sbbi.net/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "UPNPLib" and "SuperBonBon Industries" must not be
 *    used to endorse or promote products derived from this software without
 *    prior written permission. For written permission, please contact
 *    info@sbbi.net.
 *
 * 5. Products  derived from this software may not be called 
 *    "SuperBonBon Industries", nor may "SBBI" appear in their name, 
 *    without prior written permission of SuperBonBon Industries.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT,INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software  consists of voluntary contributions made by many individuals
 * on behalf of SuperBonBon Industries. For more information on 
 * SuperBonBon Industries, please see <http://www.sbbi.net/>.
 */
package net.sbbi.upnp;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;

/**
 * Simple SAX handler for UPNP service event message parsing, this message is in SOAP format
 * @author <a href="mailto:superbonbon@sbbi.net">SuperBonBon</a>
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class ServiceEventMessageParser extends org.xml.sax.helpers.DefaultHandler {

  private boolean readPropertyName = false;
  
  private String currentPropName = null;
  
  private final Map changedStateVars = new HashMap();

  protected ServiceEventMessageParser() {
  }

  public Map getChangedStateVars() {
    return changedStateVars;
  }

  public void characters( char[] ch, int start, int length ) {
    if ( currentPropName != null ) {
      String origChars = (String)changedStateVars.get( currentPropName );
      String newChars = new String( ch, start, length );
      if ( origChars == null ) {
        changedStateVars.put( currentPropName, newChars );
      } else {
        changedStateVars.put( currentPropName, origChars + newChars );
      }
    }
  }

  public void startElement( String uri, String localName, String qName, Attributes attributes ) {
    if ( localName.equals( "property" ) ) {
      readPropertyName = true;
    } else if ( readPropertyName ) {
      currentPropName = localName;
    }
  }
  
  public void endElement( String uri, String localName, String qName ) {
    if ( currentPropName != null && localName.equals( currentPropName ) ) {
      readPropertyName = false;
      currentPropName = null;
    }
  }
  
}


