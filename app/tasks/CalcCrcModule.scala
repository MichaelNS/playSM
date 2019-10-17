package tasks

import play.api.inject.SimpleModule
import play.api.inject._

class CalcCrcModule extends SimpleModule(bind[CalcCrcTask].toSelf.eagerly())
