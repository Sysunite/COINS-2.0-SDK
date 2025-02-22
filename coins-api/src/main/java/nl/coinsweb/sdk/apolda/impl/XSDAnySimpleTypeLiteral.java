/**
 * MIT License
 *
 * Copyright (c) 2016 Bouw Informatie Raad
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 **/
package nl.coinsweb.sdk.apolda.impl;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import nl.coinsweb.sdk.exceptions.CoinsObjectCastNotAllowedException;

/**
 * @author Bastiaan Bijl, Sysunite 2016
 */
public class XSDAnySimpleTypeLiteral {

  private Literal literal;

  public XSDAnySimpleTypeLiteral(Literal literal) {
    this.literal = literal;
  }

  public <T> T as(Class<T> clazz) {
    RDFDatatype datatype = TypeMapper.getInstance().getTypeByClass(clazz);
    if(datatype != null) {
      T object = (T) datatype.parse(literal.getLexicalForm());
      return object;
    }
    throw new CoinsObjectCastNotAllowedException("Failed to cast "+literal.getString()+" to "+clazz.getCanonicalName()+".");
  }
  public boolean canAs(Class clazz) {
    try {
      as(clazz);
    } catch (CoinsObjectCastNotAllowedException e) {
      return false;
    }
    return true;
  }
}
