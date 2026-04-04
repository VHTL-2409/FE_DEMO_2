import fitz

pdf_path = r"c:\Users\Administrator\Desktop\FE_DEMO\pdf_mau_3.pdf"
out_path = r"c:\Users\Administrator\Desktop\FE_DEMO\pdf_mau_3_extracted.txt"

doc = fitz.open(pdf_path)

with open(out_path, "w", encoding="utf-8") as out:
    out.write(f"Total pages: {doc.page_count}\n")
    out.write(f"Metadata: {doc.metadata}\n")
    out.write("=" * 80 + "\n")

    for page_num in range(doc.page_count):
        page = doc[page_num]
        out.write(f"\n{'='*80}\n")
        out.write(f"PAGE {page_num + 1}\n")
        out.write(f"{'='*80}\n")

        text = page.get_text("text")
        out.write(text)
        out.write("\n")

        blocks = page.get_text("blocks")
        if blocks:
            out.write(f"\n--- Blocks on page {page_num + 1} ---\n")
            for b in blocks:
                x0, y0, x1, y1, content, block_no, block_type = b
                out.write(f"[Block {block_no}] Type={block_type} Y={y0:.1f}-{y1:.1f}: {repr(content[:300])}\n")

        images = page.get_images()
        if images:
            out.write(f"\n--- Images on page {page_num + 1}: {len(images)} image(s) ---\n")
            for img in images:
                out.write(f"  Image: {img}\n")

        drawings = page.get_drawings()
        if drawings:
            out.write(f"\n--- Drawings on page {page_num + 1}: {len(drawings)} drawing(s) ---\n")

        tables = page.find_tables()
        if tables:
            out.write(f"\n--- Tables on page {page_num + 1} ---\n")
            for table in tables:
                out.write(f"  Table: {table}\n")

doc.close()

print(f"Done. Output written to {out_path}")
