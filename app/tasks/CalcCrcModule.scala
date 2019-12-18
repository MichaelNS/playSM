/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package tasks

import play.api.inject.SimpleModule
import play.api.inject._

class CalcCrcModule extends SimpleModule(bind[CalcCrcTask].toSelf.eagerly())
