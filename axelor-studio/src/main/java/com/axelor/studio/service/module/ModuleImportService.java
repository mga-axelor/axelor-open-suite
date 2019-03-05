/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.studio.service.module;

import com.axelor.app.AppSettings;
import com.axelor.common.FileUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.studio.db.ModuleBuilder;
import com.axelor.studio.exception.IExceptionMessage;
import com.google.common.io.Files;
import com.google.inject.persist.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class ModuleImportService {

  private static final String MODULE_PATTERN = "axelor(-[a-z]+)+$";

  private static final List<String> moduleStructure =
      Arrays.asList(
          new String[] {"build.gradle", "src/main/java", "src/test/java", "src/main/resources"});

  @Transactional
  public void importModule(ModuleBuilder moduleBuilder)
      throws ZipException, IOException, AxelorException {

    if (moduleBuilder.getImportMetaFile() == null) {
      return;
    }

    MetaFile metaFile = moduleBuilder.getImportMetaFile();
    File file = MetaFiles.getPath(metaFile).toFile();
    validateFile(file);

    String moduleName = moduleBuilder.getName();
    if (!moduleName.matches(MODULE_PATTERN)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(IExceptionMessage.INVALID_MODULE_ZIP));
    }

    File moduleDir = new File(getModuleDir(), moduleName);

    if (!moduleDir.exists()) {
      moduleDir.mkdirs();
    }

    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));

    ZipEntry entry = zipInputStream.getNextEntry();

    while (entry != null) {
      String name = entry.getName();
      File entryFile = FileUtils.getFile(moduleDir.getAbsolutePath(), name.split("/"));
      if (!entryFile.exists()) {
        Files.createParentDirs(entryFile);
      }
      IOUtils.copy(zipInputStream, new FileOutputStream(entryFile));
      entry = zipInputStream.getNextEntry();
    }

    zipInputStream.close();
  }

  public void validateFile(File file) throws AxelorException, ZipException, IOException {

    String extension = FilenameUtils.getExtension(file.getName());
    if (extension == null || !extension.equals("zip")) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(IExceptionMessage.INVALID_ZIP));
    }

    ZipInputStream inputStream = new ZipInputStream(new FileInputStream(file));
    ZipEntry entry = inputStream.getNextEntry();
    while (entry != null) {
      boolean valid = false;
      for (String structure : moduleStructure) {
        if (entry.getName().contains(structure)) {
          valid = true;
        }
      }
      if (!valid) {
        inputStream.close();
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.INVALID_ZIP_ENTRY),
            entry.getName());
      }
      entry = inputStream.getNextEntry();
    }

    inputStream.close();
  }

  /**
   * Method to get build directory from property setting.
   *
   * @return
   * @throws AxelorException
   */
  public File getSourceDir() throws AxelorException {

    String sourcePath = AppSettings.get().get("studio.source.dir");

    if (sourcePath != null) {
      File sourceDir = new File(sourcePath);
      if (sourceDir.exists() && sourceDir.isDirectory()) {
        return sourceDir;
      }
    }

    throw new AxelorException(
        TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
        I18n.get(IExceptionMessage.NO_SOURCE_DIR));
  }

  public File getModuleDir() throws AxelorException {

    File moduleDir = new File(getSourceDir(), "modules");

    if (!moduleDir.exists()) {
      moduleDir.mkdirs();
    }

    return moduleDir;
  }
}
