/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.multideposit2.model

import java.nio.file.Path

sealed abstract class AvVocabulary(val vocabulary: String)
case object Audio extends AvVocabulary("http://schema.org/AudioObject")
case object Video extends AvVocabulary("http://schema.org/VideoObject")

sealed abstract class FileMetadata(val filepath: Path,
                                   val mimeType: MimeType)
case class DefaultFileMetadata(override val filepath: Path,
                               override val mimeType: MimeType,
                               title: Option[String] = Option.empty,
                               accessibleTo: Option[FileAccessRights.Value] = Option.empty
                              ) extends FileMetadata(filepath, mimeType)
case class AVFileMetadata(override val filepath: Path,
                          override val mimeType: MimeType,
                          vocabulary: AvVocabulary,
                          title: String,
                          accessibleTo: FileAccessRights.Value,
                          subtitles: Set[Subtitles] = Set.empty
                         ) extends FileMetadata(filepath, mimeType)

case class FileDescriptor(title: Option[String] = Option.empty,
                          accessibility: Option[FileAccessRights.Value] = Option.empty)
