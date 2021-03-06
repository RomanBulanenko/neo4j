/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal

import org.neo4j.graphdb.ConstraintViolationException
import spi.QueryContext
import org.neo4j.graphdb.TransactionFailureException
import org.neo4j.cypher.NodeStillHasRelationshipsException
import org.neo4j.cypher.internal.helpers.IsCollection

/**
 * An iterator that decorates an inner iterator, and calls close() on the QueryContext once
 * the inner iterator is empty.
 */
class ClosingIterator(inner: Iterator[collection.Map[String, Any]], queryContext: QueryContext) extends Iterator[Map[String, Any]] {
  private var closed: Boolean = false
  lazy val still_has_relationships = "Node record Node\\[(\\d),.*] still has relationships".r

  def hasNext: Boolean = failIfThrows {
    val innerHasNext: Boolean = inner.hasNext
    if (!innerHasNext) {
      close()
    }
    innerHasNext
  }

  def next(): Map[String, Any] = failIfThrows {
    val result: Map[String, Any] = inner.next().mapValues(materialize).toMap
    if (!inner.hasNext) {
      close()
    }
    result
  }

  private def materialize(v: Any): Any = v match {
    case (x: Stream[_])   => x.map(materialize).toList
    case (x: Map[_, _])   => x.mapValues(materialize)
    case (x: Iterable[_]) => x.map(materialize)
    case x                => x
  }

  def close() {
    translateException {
      if (!closed) {
        closed = true
        queryContext.close(success = true)
      }
    }
  }

  private def translateException[U](f: => U): U = try {
    f
  } catch {
    case e: TransactionFailureException => {

      var cause: Throwable = e
      while (cause.getCause != null) {
        cause = cause.getCause
        if (cause.isInstanceOf[ConstraintViolationException]) {
          cause.getMessage match {
            case still_has_relationships(id) => throw new NodeStillHasRelationshipsException(id.toLong, e)
            case _                           => throw e
          }
        }
      }

      throw e
    }
  }

  private def failIfThrows[U](f: => U): U = try {
    f
  } catch {
    case t: Throwable if !closed =>
      queryContext.close(success = false)
      throw t
  }
}