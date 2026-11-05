window.runSlideLayoutCheck = function runSlideLayoutCheck() {
  const overlapIssues = [];
  const slideNodes = Array.from(document.querySelectorAll('.slide'));

  function rectOf(element) {
    const rect = element.getBoundingClientRect();
    return { left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom };
  }

  function textRects(root) {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const rects = [];
    while (walker.nextNode()) {
      const node = walker.currentNode;
      if (!node.textContent?.trim()) continue;
      const range = document.createRange();
      range.selectNodeContents(node);
      for (const rect of Array.from(range.getClientRects())) {
        if (rect.width > 0 && rect.height > 0) {
          rects.push({
            text: node.textContent.trim().replace(/\s+/g, ' ').slice(0, 80),
            // Range boxes include font ascenders/descenders beyond tight line-height.
            overhang: parseFloat(getComputedStyle(node.parentElement).fontSize) * 0.2,
            left: rect.left,
            right: rect.right,
            top: rect.top,
            bottom: rect.bottom,
          });
        }
      }
    }
    return rects;
  }

  slideNodes.forEach((slide, index) => {
    const slideId = slide.dataset.slideId || `slide-${index + 1}`;
    const contentColumns = Array.from(slide.querySelectorAll('[data-layout-content]'));
    const reservedAreas = Array.from(slide.querySelectorAll('[data-layout-reserved]'));
    if (contentColumns.length === 0) {
      overlapIssues.push(`${slideId}: missing a marked content column`);
      return;
    }

    for (const column of contentColumns) {
      const name = column.dataset.layoutContent || 'content';
      const bounds = rectOf(column);
      for (const text of textRects(column)) {
        if (
          text.left < bounds.left - 2 ||
          text.right > bounds.right + 2 ||
          text.top < bounds.top - text.overhang ||
          text.bottom > bounds.bottom + text.overhang
        ) {
          overlapIssues.push(
            `${slideId}: text leaves "${name}" column: "${text.text}" (${text.left.toFixed(1)},${text.top.toFixed(1)}–${text.right.toFixed(1)},${text.bottom.toFixed(1)} vs ${bounds.left.toFixed(1)},${bounds.top.toFixed(1)}–${bounds.right.toFixed(1)},${bounds.bottom.toFixed(1)})`,
          );
        }
      }
    }

    const slideText = textRects(slide);
    const frame = rectOf(slide);
    for (const text of slideText) {
      if (
        text.left < frame.left - 2 ||
        text.right > frame.right + 2 ||
        text.top < frame.top - text.overhang ||
        text.bottom > frame.bottom + text.overhang
      ) {
        overlapIssues.push(`${slideId}: text leaves the slide frame: "${text.text}"`);
      }
    }
    for (const reserved of reservedAreas) {
      const name = reserved.dataset.layoutReserved || 'reserved graphic';
      const bounds = rectOf(reserved);
      for (const text of slideText) {
        const intersects =
          text.left < bounds.right &&
          text.right > bounds.left &&
          text.top < bounds.bottom &&
          text.bottom > bounds.top;
        if (intersects) {
          overlapIssues.push(
            `${slideId}: text overlaps "${name}": "${text.text}" (${text.left.toFixed(1)},${text.top.toFixed(1)}–${text.right.toFixed(1)},${text.bottom.toFixed(1)} vs ${bounds.left.toFixed(1)},${bounds.top.toFixed(1)}–${bounds.right.toFixed(1)},${bounds.bottom.toFixed(1)})`,
          );
        }
      }
    }
  });

  return { slideCount: slideNodes.length, overlapIssues: [...new Set(overlapIssues)] };
};
