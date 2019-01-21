package ru.ns.model

/**
  * Created by ns on 08.02.2017.
  */
case class FileCardSt(id: String,
                      storeName: String,
                      fParent: String,
                      fName: String,
                      fExtension: Option[String] = None,
                      fCreationDate: java.time.LocalDateTime,
                      fLastModifiedDate: java.time.LocalDateTime,
                      fSize: Option[Long] = None,
                      fMimeTypeJava: Option[String] = None,
                      fNameLc: String
                     )

