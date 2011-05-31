/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.ldap.responses;



import java.util.List;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.ControlDecoder;



/**
 * A Extended result indicates the status of an Extended operation and any
 * additional information associated with the Extended operation, including the
 * optional response name and value. These can be retrieved using the
 * {@link #getOID} and {@link #getValue} methods respectively.
 */
public interface ExtendedResult extends Result
{
  /**
   * {@inheritDoc}
   */
  ExtendedResult addControl(Control control)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * {@inheritDoc}
   */
  ExtendedResult addReferralURI(String uri)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * {@inheritDoc}
   */
  Throwable getCause();



  /**
   * {@inheritDoc}
   */
  <C extends Control> C getControl(ControlDecoder<C> decoder,
      DecodeOptions options) throws NullPointerException, DecodeException;



  /**
   * {@inheritDoc}
   */
  List<Control> getControls();



  /**
   * {@inheritDoc}
   */
  String getDiagnosticMessage();



  /**
   * {@inheritDoc}
   */
  String getMatchedDN();



  /**
   * Returns the numeric OID, if any, associated with this extended result.
   *
   * @return The numeric OID associated with this extended result, or {@code
   *         null} if there is no OID.
   */
  String getOID();



  /**
   * {@inheritDoc}
   */
  List<String> getReferralURIs();



  /**
   * {@inheritDoc}
   */
  ResultCode getResultCode();



  /**
   * Returns the value, if any, associated with this extended result. Its format
   * is defined by the specification of this extended result.
   *
   * @return The value associated with this extended result, or {@code null} if
   *         there is no value.
   */
  ByteString getValue();



  /**
   * Returns {@code true} if this extended result has a value. In some
   * circumstances it may be useful to determine if a extended result has a
   * value, without actually calculating the value and incurring any performance
   * costs.
   *
   * @return {@code true} if this extended result has a value, or {@code false}
   *         if there is no value.
   */
  boolean hasValue();



  /**
   * {@inheritDoc}
   */
  boolean isReferral();



  /**
   * {@inheritDoc}
   */
  boolean isSuccess();



  /**
   * {@inheritDoc}
   */
  ExtendedResult setCause(Throwable cause) throws UnsupportedOperationException;



  /**
   * {@inheritDoc}
   */
  ExtendedResult setDiagnosticMessage(String message)
      throws UnsupportedOperationException;



  /**
   * {@inheritDoc}
   */
  ExtendedResult setMatchedDN(String dn) throws UnsupportedOperationException;



  /**
   * {@inheritDoc}
   */
  ExtendedResult setResultCode(ResultCode resultCode)
      throws UnsupportedOperationException, NullPointerException;
}