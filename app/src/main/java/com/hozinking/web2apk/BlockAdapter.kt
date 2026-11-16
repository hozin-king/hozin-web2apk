package com.hozinking.web2apk

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File

/** Adapter canvas: render tiap blok sebagai view Android native (tanpa WebView). */
class BlockAdapter(
    private val context: Context,
    val blocks: MutableList<Block>,
    private val assetsDir: File
) : RecyclerView.Adapter<BlockAdapter.VH>() {

    var previewMode: Boolean = false
    var selectedPosition: Int = -1
    var onBlockClick: ((Int) -> Unit)? = null
    var onButtonClick: ((Block) -> Unit)? = null

    class VH(val root: View) : RecyclerView.ViewHolder(root)

    override fun getItemViewType(position: Int): Int = blocks[position].type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val type = BlockType.values()[viewType]
        val root = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        when (type) {
            BlockType.JUDUL -> root.addView(TextView(context))
            BlockType.TEKS -> root.addView(TextView(context))
            BlockType.GAMBAR -> root.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
            })
            BlockType.TOMBOL -> root.addView(MaterialButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            })
            BlockType.KARTU -> root.addView(MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                radius = dp(12).toFloat()
                addView(TextView(context).apply { setPadding(dp(16), dp(16), dp(16), dp(16)) })
            })
            BlockType.PEMBATAS -> root.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                ).apply { topMargin = dp(8); bottomMargin = dp(8) }
            })
            BlockType.SPASI -> root.addView(Space(context))
            BlockType.VIDEO -> root.addView(FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(200)
                )
                setBackgroundColor(Color.BLACK)
                addView(TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = Gravity.CENTER }
                    gravity = Gravity.CENTER
                })
            })
            BlockType.NAVBAR -> root.addView(LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#1B2530"))
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(context))
            })
            BlockType.FOOTER -> root.addView(TextView(context).apply {
                gravity = Gravity.CENTER
            })
        }
        return VH(root)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val block = blocks[position]
        val root = holder.root as LinearLayout
        val selected = !previewMode && position == selectedPosition
        root.background = if (selected) {
            GradientDrawable().apply {
                setStroke(dp(2), Color.parseColor("#2DD4BF"))
                cornerRadius = dp(8).toFloat()
            }
        } else null
        val child = root.getChildAt(0)
        when (block.type) {
            BlockType.JUDUL -> (child as TextView).apply {
                text = block.text.ifEmpty { "Judul" }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, block.textSizeSp.toFloat())
                setTypeface(null, Typeface.BOLD)
                setTextColor(parse(block.colorHex))
            }
            BlockType.TEKS -> (child as TextView).apply {
                text = block.text.ifEmpty { "Teks" }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, block.textSizeSp.toFloat())
                setTextColor(parse(block.colorHex))
            }
            BlockType.GAMBAR -> {
                val iv = child as ImageView
                val f = if (block.imageName.isNotBlank()) File(assetsDir, block.imageName) else null
                if (f != null && f.exists()) {
                    iv.setImageBitmap(decodeSampled(f, 1024))
                    iv.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    iv.setImageDrawable(null)
                    iv.setBackgroundColor(Color.parseColor("#24313D"))
                    iv.minimumHeight = dp(120)
                }
            }
            BlockType.TOMBOL -> (child as MaterialButton).apply {
                text = block.text.ifEmpty { "Tombol" }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, block.textSizeSp.toFloat())
                setTextColor(parse(block.colorHex))
                backgroundTintList = android.content.res.ColorStateList.valueOf(parse(block.bgHex))
                setOnClickListener {
                    if (previewMode) onButtonClick?.invoke(block)
                    else onBlockClick?.invoke(holder.bindingAdapterPosition)
                }
            }
            BlockType.KARTU -> {
                val card = child as MaterialCardView
                card.setCardBackgroundColor(parse(block.bgHex))
                (card.getChildAt(0) as TextView).apply {
                    text = block.text.ifEmpty { "Kartu" }
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, block.textSizeSp.toFloat())
                    setTextColor(parse(block.colorHex))
                }
            }
            BlockType.PEMBATAS -> child.setBackgroundColor(parse(block.bgHex))
            BlockType.SPASI -> {
                child.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(block.heightDp)
                )
            }
            BlockType.VIDEO -> {
                val frame = child as FrameLayout
                (frame.getChildAt(0) as TextView).apply {
                    text = "▶  " + (block.link.ifBlank { "Video — isi link di properti" })
                    setTextColor(Color.WHITE)
                    textSize = 14f
                }
            }
            BlockType.NAVBAR -> {
                val bar = child as LinearLayout
                (bar.getChildAt(0) as TextView).apply {
                    text = block.text.ifEmpty { "Situs Saya" }
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, block.textSizeSp.toFloat())
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(parse(block.colorHex))
                }
                (bar.getChildAt(1) as TextView).apply {
                    text = if (block.link.isNotBlank()) "Menu" else ""
                    setTextColor(Color.parseColor("#2DD4BF"))
                    textSize = 14f
                }
            }
            BlockType.FOOTER -> (child as TextView).apply {
                text = block.text.ifEmpty { "Footer" }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, block.textSizeSp.toFloat())
                setTextColor(parse(block.colorHex))
            }
        }
        if (block.type != BlockType.TOMBOL) {
            holder.root.setOnClickListener {
                if (!previewMode) onBlockClick?.invoke(holder.bindingAdapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = blocks.size

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()

    private fun parse(hex: String): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(Color.WHITE)

    private fun decodeSampled(file: File, maxPx: Int): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val w = bounds.outWidth
        while (w / sample > maxPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }
}
