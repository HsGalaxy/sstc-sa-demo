import { useCallback, useEffect, useState } from 'react'
import { api, type IntegrationStatus, type PresentationView, type SlideView } from '../../api/client'
import { useToast } from '../../app/ToastContext'
import { Badge, Button, Card, Empty, Field, Modal, Stat, formatDateTime, relativeTime } from '../../components/ui'

/** Readable message for anything the API client throws. */
const errorText = (err: unknown): string => (err instanceof Error ? err.message : String(err))

const THEMES = ['stc-light', 'stc-dark', 'university', 'minimal']
const LAYOUTS = ['title', 'bullets', 'two-column', 'closing']

/**
 * US02 · Toolbox.PresentationBuilding · PPTPresentationBuilder + OfficeAPIAdapter
 *
 * Slides are edited through {@code UpdateSlide(presentationID, slideID, edit)}: passing a slideId
 * updates that slide, omitting it appends a new one. Bullets travel as a semicolon separated
 * string, exactly like the shared data contract of the Office API adapter.
 */
export function PresentationsPage() {
  const toast = useToast()

  const [decks, setDecks] = useState<PresentationView[]>([])
  const [integrations, setIntegrations] = useState<IntegrationStatus | null>(null)
  const [selected, setSelected] = useState<PresentationView | null>(null)
  const [busy, setBusy] = useState(false)

  // ---- slide editor form
  const [slideId, setSlideId] = useState<number | null>(null)
  const [slideTitle, setSlideTitle] = useState('')
  const [slideBullets, setSlideBullets] = useState('')
  const [slideNotes, setSlideNotes] = useState('')
  const [slideLayout, setSlideLayout] = useState('bullets')

  // ---- new deck modal
  const [showNew, setShowNew] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newTheme, setNewTheme] = useState('stc-light')

  const loadIntegrations = useCallback(async () => {
    try {
      setIntegrations(await api.integrations())
    } catch (err) {
      toast.push(`Integrations unavailable: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadDecks = useCallback(async () => {
    try {
      setDecks(await api.presentations())
    } catch (err) {
      toast.push(`Could not load presentations: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const openDeck = useCallback(
    async (id: number) => {
      try {
        const deck = await api.presentation(id)
        setSelected(deck)
        setSlideId(null)
        setSlideTitle('')
        setSlideBullets('')
        setSlideNotes('')
        setSlideLayout('bullets')
      } catch (err) {
        toast.push(`Could not open deck: ${errorText(err)}`, 'error')
      }
    },
    [toast]
  )

  useEffect(() => {
    void loadDecks()
    void loadIntegrations()
  }, [loadDecks, loadIntegrations])

  const editSlide = (slide: SlideView) => {
    setSlideId(slide.id)
    setSlideTitle(slide.title ?? '')
    setSlideBullets(slide.bullets.join('\n'))
    setSlideNotes(slide.notes ?? '')
    setSlideLayout(slide.layout ?? 'bullets')
  }

  const resetForm = () => {
    setSlideId(null)
    setSlideTitle('')
    setSlideBullets('')
    setSlideNotes('')
    setSlideLayout('bullets')
  }

  /** UpdateSlide(presentationID, slideID, edit) — one bullet per line, joined with ';'. */
  const submitSlide = async () => {
    if (!selected) return
    const bullets = slideBullets
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0)
      .join(';')
    if (!slideTitle.trim() && bullets.length === 0) {
      toast.push('A slide needs a title or at least one bullet', 'error')
      return
    }
    setBusy(true)
    try {
      const previousRevision = selected.revision
      const updated = await api.updateSlide(
        selected.id,
        {
          title: slideTitle.trim(),
          bullets,
          notes: slideNotes,
          layout: slideLayout
        },
        slideId ?? undefined
      )
      setSelected(updated)
      if (updated.revision === previousRevision) {
        toast.push(
          `Slide not applied: Office API quota exhausted (symbolic resultCode -1, ${integrations?.officeQuotaRemaining ?? 0} left)`,
          'error'
        )
      } else {
        toast.push(
          `${slideId ? 'Slide updated' : 'Slide added'} · deck revision ${updated.revision}`,
          'ok'
        )
      }
      resetForm()
      await loadDecks()
      await loadIntegrations()
    } catch (err) {
      toast.push(`Slide update failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const createDeck = async () => {
    if (!newTitle.trim()) {
      toast.push('A deck title is required', 'error')
      return
    }
    try {
      const created = await api.createPresentation({ title: newTitle.trim(), theme: newTheme })
      toast.push(`Deck “${created.title}” created`, 'ok')
      setShowNew(false)
      setNewTitle('')
      setNewTheme('stc-light')
      await loadDecks()
      await openDeck(created.id)
    } catch (err) {
      toast.push(`Create failed: ${errorText(err)}`, 'error')
    }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      {/* US02 header block */}
      <div>
        <h1 style={{ margin: 0, fontSize: 20, letterSpacing: '-0.4px' }}>Presentations</h1>
        <div className="muted" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.4px' }}>
          US02 · Toolbox.PresentationBuilding · PPTPresentationBuilder + OfficeAPIAdapter
        </div>
      </div>

      <div className="grid grid-4">
        <Stat label="Decks" value={decks.length} accent="s04" />
        <Stat
          label="Slides"
          value={decks.reduce((total, deck) => total + deck.slideCount, 0)}
          delta="across every deck"
          accent="s04"
        />
        <Stat label="Office API provider" value={integrations?.officeProvider ?? '—'} accent="s04" />
        <Stat
          label="Office quota"
          value={`${integrations?.officeQuotaRemaining ?? 0} / ${integrations?.officeQuotaTotal ?? 0}`}
          delta="shared with the document editor"
          accent="s04"
        />
      </div>

      <Card
        title="Deck library"
        hint="Slide decks built through PPTPresentationBuilder and stored with their revision."
        actions={
          <Button variant="primary" onClick={() => setShowNew(true)}>
            New deck
          </Button>
        }
        padded={false}
      >
        {decks.length === 0 ? (
          <Empty>No decks yet — create the first one.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Theme</th>
                  <th>Owner</th>
                  <th>Slides</th>
                  <th>Rev</th>
                  <th>Last edited</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {decks.map((deck) => (
                  <tr key={deck.id} onClick={() => void openDeck(deck.id)} style={{ cursor: 'pointer' }}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{deck.title}</div>
                      <div className="soft mono">#{deck.id}</div>
                    </td>
                    <td>
                      <Badge tone="s04">{deck.theme ?? 'stc-light'}</Badge>
                    </td>
                    <td>{deck.ownerName ?? '—'}</td>
                    <td>{deck.slideCount}</td>
                    <td>{deck.revision}</td>
                    <td title={formatDateTime(deck.lastEditedAt)}>{relativeTime(deck.lastEditedAt)}</td>
                    <td>
                      <Button size="sm" onClick={() => void openDeck(deck.id)}>
                        Open
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {selected ? (
        <>
          <div className="grid grid-2-1">
            <Card
              title={`Slides of “${selected.title}”`}
              hint="Bullets come from the semicolon separated field of the slide record."
              actions={<Badge tone="s04">revision {selected.revision}</Badge>}
            >
              {selected.slides.length === 0 ? (
                <Empty>This deck has no slides yet — add the first one on the right.</Empty>
              ) : (
                <div className="grid grid-2">
                  {selected.slides.map((slide) => (
                    <div key={slide.id} className="card" style={{ padding: 14 }}>
                      <div className="chip-row" style={{ marginBottom: 8 }}>
                        <Badge tone="s04">slide {slide.slideNo}</Badge>
                        <Badge>{slide.layout ?? 'bullets'}</Badge>
                        <div className="topbar-spacer" />
                        <Button size="sm" onClick={() => editSlide(slide)}>
                          Edit
                        </Button>
                      </div>
                      <div style={{ fontWeight: 700, marginBottom: 6 }}>{slide.title ?? 'Untitled slide'}</div>
                      {slide.bullets.length === 0 ? (
                        <div className="soft" style={{ fontSize: 12 }}>
                          No bullets.
                        </div>
                      ) : (
                        <ul style={{ margin: '0 0 8px 18px', padding: 0, fontSize: 13 }}>
                          {slide.bullets.map((bullet, index) => (
                            <li key={index}>{bullet}</li>
                          ))}
                        </ul>
                      )}
                      {slide.notes && (
                        <div className="muted" style={{ fontSize: 12, borderTop: '1px dashed #e2e8f0', paddingTop: 6 }}>
                          Notes: {slide.notes}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <Card
              title={slideId ? `Update slide #${slideId}` : 'Add slide'}
              hint="UpdateSlide(presentationID, slideID, edit) — a new slide when slideID is omitted."
            >
              <Field label="Slide title">
                <input
                  className="input"
                  value={slideTitle}
                  onChange={(event) => setSlideTitle(event.target.value)}
                  placeholder="Architecture overview"
                />
              </Field>
              <Field label="Bullets (one per line)">
                <textarea
                  className="textarea"
                  rows={6}
                  value={slideBullets}
                  onChange={(event) => setSlideBullets(event.target.value)}
                  placeholder={'Four sub-systems\nShared dashboard\nSpring Boot backend'}
                />
              </Field>
              <Field label="Notes">
                <textarea
                  className="textarea"
                  rows={3}
                  value={slideNotes}
                  onChange={(event) => setSlideNotes(event.target.value)}
                />
              </Field>
              <Field label="Layout">
                <select
                  className="select"
                  value={slideLayout}
                  onChange={(event) => setSlideLayout(event.target.value)}
                >
                  {LAYOUTS.map((layout) => (
                    <option key={layout} value={layout}>
                      {layout}
                    </option>
                  ))}
                </select>
              </Field>
              <div className="toolbar">
                <Button variant="primary" disabled={busy} onClick={() => void submitSlide()}>
                  {slideId ? 'Save slide' : 'Add slide'}
                </Button>
                <Button variant="ghost" disabled={busy} onClick={resetForm}>
                  Clear form
                </Button>
                <Button variant="ghost" disabled={busy} onClick={() => void openDeck(selected.id)}>
                  Reload deck
                </Button>
              </div>
            </Card>
          </div>
        </>
      ) : (
        <Card title="Deck detail">
          <Empty>Select a deck to list its slides and edit them.</Empty>
        </Card>
      )}

      {showNew && (
        <Modal
          title="New presentation"
          onClose={() => setShowNew(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setShowNew(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={() => void createDeck()}>
                Create deck
              </Button>
            </>
          }
        >
          <Field label="Title">
            <input
              className="input"
              value={newTitle}
              onChange={(event) => setNewTitle(event.target.value)}
              placeholder="Sprint 4 review"
            />
          </Field>
          <Field label="Theme">
            <select className="select" value={newTheme} onChange={(event) => setNewTheme(event.target.value)}>
              {THEMES.map((theme) => (
                <option key={theme} value={theme}>
                  {theme}
                </option>
              ))}
            </select>
          </Field>
        </Modal>
      )}
    </div>
  )
}
