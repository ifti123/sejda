/*
 * Created on 02/jul/2011
 *
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
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
package org.sejda.impl.itext;

import static org.sejda.core.notification.dsl.ApplicationEventsNotifier.notifyEvent;
import static org.sejda.core.support.io.IOUtils.createTemporaryPdfBuffer;
import static org.sejda.core.support.io.model.FileOutput.file;
import static org.sejda.core.support.util.ComponentsUtility.nullSafeCloseQuietly;
import static org.sejda.impl.itext.util.ITextUtils.nullSafeClosePdfReader;
import static org.sejda.impl.itext.util.ViewerPreferencesUtils.getPageMode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sejda.core.exception.TaskException;
import org.sejda.core.manipulation.model.input.PdfSource;
import org.sejda.core.manipulation.model.input.PdfSourceOpener;
import org.sejda.core.manipulation.model.parameter.SetPagesTransitionParameters;
import org.sejda.core.manipulation.model.pdf.transition.PdfPageTransition;
import org.sejda.core.manipulation.model.pdf.viewerpreferences.PdfPageMode;
import org.sejda.core.manipulation.model.task.Task;
import org.sejda.core.support.io.OutputWriters;
import org.sejda.core.support.io.SingleOutputWriter;
import org.sejda.impl.itext.component.PdfStamperHandler;
import org.sejda.impl.itext.component.input.PdfSourceOpeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.pdf.PdfReader;

/**
 * Task that applies pages transitions to an input document.
 * 
 * @author Andrea Vacondio
 * 
 */
public class SetPagesTransitionTask implements Task<SetPagesTransitionParameters> {

    private static final Logger LOG = LoggerFactory.getLogger(SetPagesTransitionTask.class);

    private PdfReader reader = null;
    private PdfStamperHandler stamperHandler = null;
    private SingleOutputWriter outputWriter = OutputWriters.newSingleOutputWriter();
    private PdfSourceOpener<PdfReader> sourceOpener;

    public void before(SetPagesTransitionParameters parameters) {
        sourceOpener = PdfSourceOpeners.newPartialReadOpener();
    }

    public void execute(SetPagesTransitionParameters parameters) throws TaskException {
        PdfSource source = parameters.getSource();
        LOG.debug("Opening {} ", source);
        reader = source.open(sourceOpener);

        File tmpFile = createTemporaryPdfBuffer();
        LOG.debug("Created output temporary buffer {} ", tmpFile);
        stamperHandler = new PdfStamperHandler(reader, tmpFile, parameters.getVersion());

        stamperHandler.setCompressionOnStamper(parameters.isCompressXref());
        stamperHandler.setCreatorOnStamper(reader);
        if (parameters.isFullScreen()) {
            LOG.debug("Setting fullscreen mode");
            stamperHandler.setViewerPreferencesOnStamper(getPageMode(PdfPageMode.FULLSCREEN));
        }

        Map<Integer, PdfPageTransition> transitions = getTransitionsMap(parameters, reader.getNumberOfPages());
        LOG.debug("Applying {} transitions", transitions.size());
        int currentStep = 0;
        for (Entry<Integer, PdfPageTransition> entry : transitions.entrySet()) {
            LOG.trace("Applying transition {} to page {}", entry.getValue(), entry.getKey());
            stamperHandler.setTransitionOnStamper(entry.getKey(), entry.getValue());
            notifyEvent().stepsCompleted(++currentStep).outOf(transitions.size());
        }

        nullSafeClosePdfReader(reader);
        nullSafeCloseQuietly(stamperHandler);

        outputWriter.flushSingleOutput(file(tmpFile).name(parameters.getOutputName()), parameters.getOutput(),
                parameters.isOverwrite());

        LOG.debug("Transitions set on {}", parameters.getOutput());
    }

    public void after() {
        nullSafeClosePdfReader(reader);
        nullSafeCloseQuietly(stamperHandler);
    }

    /**
     * @param parameters
     * @param totalPages
     * @return a map containing all the transitions to apply considering the default transition if there is one.
     */
    private Map<Integer, PdfPageTransition> getTransitionsMap(SetPagesTransitionParameters parameters, int totalPages) {
        Map<Integer, PdfPageTransition> map = new HashMap<Integer, PdfPageTransition>();
        if (parameters.getDefaultTransition() != null) {
            for (int i = 1; i <= totalPages; i++) {
                map.put(i, parameters.getDefaultTransition());
            }
        }
        map.putAll(parameters.getTransitions());
        return map;
    }
}