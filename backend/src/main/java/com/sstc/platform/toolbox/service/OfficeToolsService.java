package com.sstc.platform.toolbox.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import com.sstc.platform.toolbox.config.ToolboxProperties;
import com.sstc.platform.toolbox.domain.DocDocument;
import com.sstc.platform.toolbox.domain.DocEdit;
import com.sstc.platform.toolbox.domain.Presentation;
import com.sstc.platform.toolbox.domain.Slide;
import com.sstc.platform.toolbox.repo.*;
import com.sstc.platform.toolbox.web.ToolboxRequests;
import com.sstc.platform.toolbox.web.ToolboxViews;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * S04 - Office Tools Container.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code Toolbox.DocumentEditing} - Word document creation and editing (US01)</li>
 *   <li>{@code Toolbox.PresentationBuilding} - slide deck creation and update (US02)</li>
 * </ul>
 *
 * <p>The quota check follows the execution view documented for the sequence diagrams:
 * {@code UpdateDocument} first calls {@code GetQuota(apiName)} and only calls
 * {@code RunOfficeOperation(edit)} when quota is available; otherwise it returns a symbolic
 * non-zero result code.</p>
 */
@Service
public class OfficeToolsService {

    /** Result code returned when the simulated Office API quota is exhausted. */
    public static final int RESULT_QUOTA_EXHAUSTED = -1;

    public static final int RESULT_OK = 0;

    private final DocumentsRepo documents;

    private final DocEditsRepo docEdits;

    private final PresentationsRepo presentations;

    private final SlidesRepo slides;

    private final ToolboxProperties properties;

    private final UserDirectory directory;

    private final AuditService auditService;

    public OfficeToolsService(DocumentsRepo documents,
                              DocEditsRepo docEdits,
                              PresentationsRepo presentations,
                              SlidesRepo slides,
                              ToolboxProperties properties,
                              UserDirectory directory,
                              AuditService auditService) {
        this.documents = documents;
        this.docEdits = docEdits;
        this.presentations = presentations;
        this.slides = slides;
        this.properties = properties;
        this.directory = directory;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ US01

    @Transactional(readOnly = true)
    public List<ToolboxViews.DocumentView> listDocuments() {
        Map<Long, User> users = directory.index();
        List<ToolboxViews.DocumentView> result = new ArrayList<ToolboxViews.DocumentView>();
        for (DocDocument document : documents.findAllByOrderByLastEditedAtDesc()) {
            result.add(decorate(document, users));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ToolboxViews.DocumentView getDocument(Long id) {
        return decorate(requireDocument(id), directory.index());
    }

    @Transactional
    public ToolboxViews.DocumentView createDocument(ToolboxRequests.DocumentRequest request) {
        DocDocument document = new DocDocument();
        document.setTitle(request.title.trim());
        document.setContent(request.content == null ? "" : request.content);
        document.setTemplate(request.template == null ? "blank" : request.template);
        document.setOwnerId(CurrentUserHolder.id());
        document.setOfficeApi(properties.getOffice().getProvider());
        document.setRevision(1);
        document.setWordCount(countWords(document.getContent()));
        document.setLastEditedAt(LocalDateTime.now());
        documents.save(document);
        auditService.success("S04", "DOCUMENT_CREATE", "document", document.getTitle(),
                "created through " + document.getOfficeApi());
        return getDocument(document.getId());
    }

    /** {@code WordDocumentBuilder.UpdateDocument(documentID, edit)} - US01. */
    @Transactional
    public ToolboxViews.DocumentView updateDocument(Long id, ToolboxRequests.DocumentEditRequest request) {
        DocDocument document = requireDocument(id);
        int quota = remainingQuota();
        int resultCode = quota > 0 ? RESULT_OK : RESULT_QUOTA_EXHAUSTED;

        if (resultCode == RESULT_OK) {
            String operation = request.operation == null ? "APPEND" : request.operation.toUpperCase();
            String payload = request.payload == null ? "" : request.payload;
            if ("REPLACE".equals(operation)) {
                document.setContent(payload);
            } else if ("RENAME".equals(operation)) {
                document.setTitle(payload.isEmpty() ? document.getTitle() : payload);
            } else {
                document.setContent((document.getContent() == null ? "" : document.getContent())
                        + (payload.isEmpty() ? "" : "\n" + payload));
            }
            document.setRevision(document.getRevision() + 1);
            document.setWordCount(countWords(document.getContent()));
            document.setLastEditedAt(LocalDateTime.now());
            documents.save(document);
        }

        DocEdit edit = new DocEdit();
        edit.setDocumentId(id);
        edit.setEditorId(CurrentUserHolder.id());
        edit.setOperation(request.operation == null ? "APPEND" : request.operation.toUpperCase());
        edit.setPayload(abbreviate(request.payload));
        edit.setResultCode(resultCode);
        edit.setQuotaRemaining(Math.max(quota - 1, 0));
        edit.setAppliedAt(LocalDateTime.now());
        docEdits.save(edit);

        if (resultCode == RESULT_OK) {
            auditService.success("S04", "DOCUMENT_EDIT", "document", document.getTitle(),
                    edit.getOperation() + " applied, revision " + document.getRevision());
        } else {
            auditService.failure("S04", "DOCUMENT_EDIT", "document", document.getTitle(),
                    "quota exhausted");
        }
        return getDocument(id);
    }

    @Transactional(readOnly = true)
    public List<ToolboxViews.DocEditView> documentHistory(Long documentId) {
        requireDocument(documentId);
        Map<Long, User> users = directory.index();
        List<ToolboxViews.DocEditView> result = new ArrayList<ToolboxViews.DocEditView>();
        for (DocEdit edit : docEdits.findByDocumentIdOrderByAppliedAtDesc(documentId)) {
            ToolboxViews.DocEditView view = new ToolboxViews.DocEditView(edit);
            User editor = edit.getEditorId() == null ? null : users.get(edit.getEditorId());
            view.editorName = editor == null ? null : editor.getDisplayName();
            result.add(view);
        }
        return result;
    }

    // ------------------------------------------------------------------ US02

    @Transactional(readOnly = true)
    public List<ToolboxViews.PresentationView> listPresentations() {
        Map<Long, User> users = directory.index();
        List<ToolboxViews.PresentationView> result = new ArrayList<ToolboxViews.PresentationView>();
        for (Presentation presentation : presentations.findAllByOrderByLastEditedAtDesc()) {
            ToolboxViews.PresentationView view = decorate(presentation, users);
            view.slides = slideViews(presentation.getId());
            result.add(view);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ToolboxViews.PresentationView getPresentation(Long id) {
        ToolboxViews.PresentationView view = decorate(requirePresentation(id), directory.index());
        view.slides = slideViews(id);
        return view;
    }

    @Transactional
    public ToolboxViews.PresentationView createPresentation(ToolboxRequests.PresentationRequest request) {
        Presentation presentation = new Presentation();
        presentation.setTitle(request.title.trim());
        presentation.setTheme(request.theme == null ? "stc-light" : request.theme);
        presentation.setOwnerId(CurrentUserHolder.id());
        presentation.setOfficeApi(properties.getOffice().getProvider());
        presentation.setSlideCount(0);
        presentation.setRevision(1);
        presentation.setLastEditedAt(LocalDateTime.now());
        presentations.save(presentation);
        auditService.success("S04", "PRESENTATION_CREATE", "presentation", presentation.getTitle(),
                "deck created");
        return getPresentation(presentation.getId());
    }

    /** {@code PPTPresentationBuilder.UpdateSlide(presentationID, slideID, edit)} - US02. */
    @Transactional
    public ToolboxViews.PresentationView updateSlide(Long presentationId, Long slideId,
                                                     ToolboxRequests.SlideRequest request) {
        Presentation presentation = requirePresentation(presentationId);
        int quota = remainingQuota();
        int resultCode = quota > 0 ? RESULT_OK : RESULT_QUOTA_EXHAUSTED;

        if (resultCode == RESULT_OK) {
            Slide slide;
            if (slideId == null) {
                slide = new Slide();
                slide.setPresentationId(presentationId);
                slide.setSlideNo(presentation.getSlideCount() + 1);
            } else {
                slide = slides.findById(slideId)
                        .orElseThrow(() -> AppException.notFound("Slide " + slideId));
            }
            if (request.title != null) {
                slide.setTitle(request.title);
            }
            if (request.bullets != null) {
                slide.setBullets(request.bullets);
            }
            if (request.notes != null) {
                slide.setNotes(request.notes);
            }
            if (request.layout != null) {
                slide.setLayout(request.layout);
            }
            slides.save(slide);
            presentation.setSlideCount(slides.findByPresentationIdOrderBySlideNoAsc(presentationId).size());
            presentation.setRevision(presentation.getRevision() + 1);
            presentation.setLastEditedAt(LocalDateTime.now());
            presentations.save(presentation);
        }

        DocEdit edit = new DocEdit();
        edit.setDocumentId(presentationId);
        edit.setEditorId(CurrentUserHolder.id());
        edit.setOperation("SLIDE_UPDATE");
        edit.setPayload(abbreviate(request.title));
        edit.setResultCode(resultCode);
        edit.setQuotaRemaining(Math.max(quota - 1, 0));
        edit.setAppliedAt(LocalDateTime.now());
        docEdits.save(edit);

        if (resultCode != RESULT_OK) {
            auditService.failure("S04", "SLIDE_UPDATE", "presentation", presentation.getTitle(),
                    "quota exhausted");
        } else {
            auditService.success("S04", "SLIDE_UPDATE", "presentation", presentation.getTitle(),
                    "revision " + presentation.getRevision());
        }
        return getPresentation(presentationId);
    }

    // ------------------------------------------------------------------ status

    @Transactional(readOnly = true)
    public int remainingQuota() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        int used = 0;
        for (DocEdit edit : docEdits.findAll()) {
            if (edit.getAppliedAt() != null && edit.getAppliedAt().isAfter(startOfDay)
                    && edit.getResultCode() == RESULT_OK) {
                used++;
            }
        }
        return Math.max(properties.getOffice().getQuotaPerDay() - used, 0);
    }

    private ToolboxViews.DocumentView decorate(DocDocument document, Map<Long, User> users) {
        ToolboxViews.DocumentView view = new ToolboxViews.DocumentView(document);
        User owner = document.getOwnerId() == null ? null : users.get(document.getOwnerId());
        view.ownerName = owner == null ? null : owner.getDisplayName();
        return view;
    }

    private ToolboxViews.PresentationView decorate(Presentation presentation, Map<Long, User> users) {
        ToolboxViews.PresentationView view = new ToolboxViews.PresentationView(presentation);
        User owner = presentation.getOwnerId() == null ? null : users.get(presentation.getOwnerId());
        view.ownerName = owner == null ? null : owner.getDisplayName();
        return view;
    }

    private List<ToolboxViews.SlideView> slideViews(Long presentationId) {
        List<ToolboxViews.SlideView> result = new ArrayList<ToolboxViews.SlideView>();
        for (Slide slide : slides.findByPresentationIdOrderBySlideNoAsc(presentationId)) {
            result.add(new ToolboxViews.SlideView(slide));
        }
        return result;
    }

    private DocDocument requireDocument(Long id) {
        return documents.findById(id).orElseThrow(() -> AppException.notFound("Document " + id));
    }

    private Presentation requirePresentation(Long id) {
        return presentations.findById(id).orElseThrow(() -> AppException.notFound("Presentation " + id));
    }

    private int countWords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 200 ? text : text.substring(0, 197) + "...";
    }
}
