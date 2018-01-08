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
package nl.knaw.dans.easy

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.{ Files, Path }

import nl.knaw.dans.easy.multideposit.model.DepositId
import org.apache.commons.io.{ Charsets, FileExistsException, FileUtils }
import resource._

import scala.collection.JavaConverters._
import scala.xml.{ Elem, PrettyPrinter, Utility, XML }

package object multideposit {

  type Datamanager = String
  type DatamanagerEmailaddress = String

  case class DepositPermissions(permissions: String, group: String)
  case class Settings(multidepositDir: Path = null,
                      stagingDir: Path = null,
                      outputDepositDir: Path = null,
                      datamanager: Datamanager = null,
                      depositPermissions: DepositPermissions = null,
                      formats: Set[String] = Set.empty[String],
                      ldap: Ldap = null) {
    override def toString: String =
      s"Settings(multideposit-dir=$multidepositDir, " +
        s"staging-dir=$stagingDir, " +
        s"output-deposit-dir=$outputDepositDir" +
        s"datamanager=$datamanager, " +
        s"deposit-permissions=$depositPermissions, " +
        s"formats=${ formats.mkString("{", ", ", "}") })"
  }

  case class EmptyInstructionsFileException(path: Path) extends Exception(s"The given instructions file in '$path' is empty")
  case class ParserFailedException(report: String, cause: Throwable = null) extends Exception(report, cause)
  case class PreconditionsFailedException(report: String, cause: Throwable = null) extends Exception(report, cause)
  case class ActionRunFailedException(report: String, cause: Throwable = null) extends Exception(report, cause)
  case class ParseException(row: Int, message: String, cause: Throwable = null) extends Exception(message, cause)
  case class ActionException(row: Int, message: String, cause: Throwable = null) extends Exception(message, cause)

  implicit class FileExtensions(val path: Path) extends AnyVal {
    /**
     * Writes a CharSequence to a file creating the file if it does not exist using the default encoding for the VM.
     *
     * @param data the content to write to the file
     */
    @throws[IOException]("in case of an I/O error")
    def write(data: String, encoding: Charset = encoding): Unit = FileUtils.write(path.toFile, data, encoding)

    /**
     * Writes the xml to `file` and prepends a simple xml header: `<?xml version="1.0" encoding="UTF-8"?>`
     *
     * @param elem     the xml to be written
     * @param encoding the encoding applied to this xml
     */
    @throws[IOException]("in case of an I/O error")
    def writeXml(elem: Elem, encoding: Charset = encoding): Unit = {
      Files.createDirectories(path.getParent)
      XML.save(path.toString, XML.loadString(new PrettyPrinter(160, 2).format(Utility.trim(elem))), encoding.toString, xmlDecl = true)
    }

    /**
     * Appends a CharSequence to a file creating the file if it does not exist using the default encoding for the VM.
     *
     * @param data the content to write to the file
     */
    @throws[IOException]("in case of an I/O error")
    def append(data: String): Unit = FileUtils.write(path.toFile, data, true)

    /**
     * Reads the contents of a file into a String using the default encoding for the VM.
     * The file is always closed.
     *
     * @return the file contents, never ``null``
     */
    @throws[IOException]("in case of an I/O error")
    def read(encoding: Charset = encoding): String = FileUtils.readFileToString(path.toFile, encoding)

    /**
     * Determines whether the ``parent`` directory contains the ``child`` element (a file or directory).
     * <p>
     * Files are normalized before comparison.
     * </p>
     *
     * Edge cases:
     * <ul>
     * <li>A ``directory`` must not be null: if null, throw IllegalArgumentException</li>
     * <li>A ``directory`` must be a directory: if not a directory, throw IllegalArgumentException</li>
     * <li>A directory does not contain itself: return false</li>
     * <li>A null child file is not contained in any parent: return false</li>
     * </ul>
     *
     * @param child the file to consider as the child.
     * @return true is the candidate leaf is under by the specified composite. False otherwise.
     */
    @throws[IOException]("if an IO error occurs while checking the files.")
    def directoryContains(child: Path): Boolean = FileUtils.directoryContains(path.toFile, child.toFile)

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the
     * specified destination file. The directory holding the destination file is
     * created if it does not exist. If the destination file exists, then this
     * method will overwrite it.
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last
     * modified date/times using File#setLastModified(long), however
     * it is not guaranteed that the operation will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param destFile the new file, must not be ``null``
     */
    @throws[NullPointerException]("if source or destination is null")
    @throws[IOException]("if source or destination is invalid")
    @throws[IOException]("if an IO error occurs during copying")
    def copyFile(destFile: Path): Unit = FileUtils.copyFile(path.toFile, destFile.toFile)

    /**
     * Copies a whole directory to a new location preserving the file dates.
     * <p>
     * This method copies the specified directory and all its child
     * directories and files to the specified destination.
     * The destination is the new location and name of the directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     * <p>
     * <strong>Note:</strong> This method tries to preserve the files' last
     * modified date/times using ``File#setLastModified(long)``, however
     * it is not guaranteed that those operations will succeed.
     * If the modification operation fails, no indication is provided.
     *
     * @param destDir the new directory, must not be ``null``
     */
    @throws[NullPointerException]("if source or destination is null")
    @throws[IOException]("if source or destination is invalid")
    @throws[IOException]("if an IO error occurs during copying")
    def copyDir(destDir: Path): Unit = FileUtils.copyDirectory(path.toFile, destDir.toFile)

    /**
     * Moves a directory.
     * <p>
     * When the destination directory is on another file system, do a "copy and delete".
     *
     * @param destDir the destination directory
     */
    @throws[NullPointerException]("if source or destination is null")
    @throws[FileExistsException]("if the destination directory exists")
    @throws[IOException]("if source or destination is invalid")
    @throws[IOException]("if an IO error occurs moving the file")
    def moveDir(destDir: Path): Unit = FileUtils.moveDirectory(path.toFile, destDir.toFile)

    /**
     * Deletes a directory recursively.
     */
    @throws[IOException]("in case deletion is unsuccessful")
    def deleteDirectory(): Unit = FileUtils.deleteDirectory(path.toFile)

    /**
     * Finds files within a given directory and its subdirectories.
     *
     * @return a ``List`` of ``java.nio.file.Path`` with the files
     */
    def listRecursively(predicate: Path => Boolean = _ => true): List[Path] = {
      managed(Files.walk(path))
        .acquireAndGet(_.iterator().asScala.filter(predicate).toList)
    }
  }

  val encoding: Charset = Charsets.UTF_8
  val bagDirName = "bag"
  val dataDirName = "data"
  val metadataDirName = "metadata"
  val instructionsFileName = "instructions.csv"
  val datasetMetadataFileName = "dataset.xml"
  val fileMetadataFileName = "files.xml"
  val propsFileName = "deposit.properties"

  private def datasetDir(depositId: DepositId)(implicit settings: Settings): String = {
    s"${ settings.multidepositDir.getFileName }-$depositId"
  }

  def multiDepositInstructionsFile(baseDir: Path): Path = {
    baseDir.resolve(instructionsFileName)
  }

  // mdDir/depositId/
  def multiDepositDir(depositId: DepositId)(implicit settings: Settings): Path = {
    settings.multidepositDir.resolve(depositId)
  }

  // mdDir/instructions.csv
  def multiDepositInstructionsFile(implicit settings: Settings): Path = {
    multiDepositInstructionsFile(settings.multidepositDir)
  }

  // stagingDir/mdDir-depositId/
  def stagingDir(depositId: DepositId)(implicit settings: Settings): Path = {
    settings.stagingDir.resolve(datasetDir(depositId))
  }

  // stagingDir/mdDir-depositId/bag/
  def stagingBagDir(depositId: DepositId)(implicit settings: Settings): Path = {
    stagingDir(depositId).resolve(bagDirName)
  }

  // stagingDir/mdDir-depositId/bag/data/
  def stagingBagDataDir(depositId: DepositId)(implicit settings: Settings): Path = {
    stagingBagDir(depositId).resolve(dataDirName)
  }

  // stagingDir/mdDir-depositId/bag/metadata/
  def stagingBagMetadataDir(depositId: DepositId)(implicit settings: Settings): Path = {
    stagingBagDir(depositId).resolve(metadataDirName)
  }

  // stagingDir/mdDir-depositId/deposit.properties
  def stagingPropertiesFile(depositId: DepositId)(implicit settings: Settings): Path = {
    stagingDir(depositId).resolve(propsFileName)
  }

  // stagingDir/mdDir-depositId/bag/metadata/dataset.xml
  def stagingDatasetMetadataFile(depositId: DepositId)(implicit settings: Settings): Path = {
    stagingBagMetadataDir(depositId).resolve(datasetMetadataFileName)
  }

  // stagingDir/mdDir-depositId/bag/metadata/files.xml
  def stagingFileMetadataFile(depositId: DepositId)(implicit settings: Settings): Path = {
    stagingBagMetadataDir(depositId).resolve(fileMetadataFileName)
  }

  // outputDepositDir/mdDir-depositId/
  def outputDepositDir(depositId: DepositId)(implicit settings: Settings): Path = {
    settings.outputDepositDir.resolve(datasetDir(depositId))
  }
}
