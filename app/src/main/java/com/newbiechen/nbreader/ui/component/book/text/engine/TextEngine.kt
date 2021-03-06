package com.newbiechen.nbreader.ui.component.book.text.engine

import android.content.Context
import com.newbiechen.nbreader.ui.component.book.text.config.TextConfig
import com.newbiechen.nbreader.ui.component.book.text.entity.TextElementArea
import com.newbiechen.nbreader.ui.component.book.text.entity.TextLine
import com.newbiechen.nbreader.ui.component.book.text.entity.TextPage
import com.newbiechen.nbreader.ui.component.book.text.entity.TextPosition
import com.newbiechen.nbreader.ui.component.book.text.entity.element.TextControlElement
import com.newbiechen.nbreader.ui.component.book.text.entity.element.TextElement
import com.newbiechen.nbreader.ui.component.book.text.entity.element.TextImageElement
import com.newbiechen.nbreader.ui.component.book.text.entity.element.TextWordElement
import com.newbiechen.nbreader.ui.component.book.text.entity.textstyle.TextAlignmentType
import com.newbiechen.nbreader.ui.component.book.text.hyphenation.TextHyphenInfo
import com.newbiechen.nbreader.ui.component.book.text.hyphenation.TextTeXHyphenator
import com.newbiechen.nbreader.ui.component.book.text.engine.cursor.TextParagraphCursor
import com.newbiechen.nbreader.ui.component.book.text.engine.cursor.TextWordCursor
import com.newbiechen.nbreader.ui.component.widget.page.PageType
import com.newbiechen.nbreader.uilts.LogHelper

/**
 *  author : newbiechen
 *  date : 2019-10-20 18:50
 *  description :文本渲染引擎
 */

class TextEngine(context: Context, textConfig: TextConfig) : BaseTextEngine(context, textConfig) {

    companion object {
        private val SPACE = charArrayOf(' ')
        private const val TAG = "TextProcessor"
    }

    // 文本模块
    private var mTextModel: TextModel? = null

    // 文本页面控制器
    private var mTextPageController: TextPageController? = null

    // 是否页面已经准备
    private var isPagePrepared = false

    private var mTextPageListener: TextPageListener? = null

    /**
     * 初始化处理器
     */
    @Synchronized
    fun init(model: TextModel) {
        // TODO:需要处理重新初始化的情况

        // 创建文本模块
        mTextModel = model

        initTextPageController(viewWidth, viewHeight)
    }

    private fun initTextPageController(width: Int, height: Int) {
        if (mTextModel == null || width == 0 || height == 0) {
            return
        }

        if (!isPagePrepared) {
            // 创建页面控制器
            mTextPageController = TextPageController(mTextModel!!, this::findPageEndCursor)

            // 设置页面监听器
            if (mTextPageListener != null) {
                mTextPageController!!.setTextPageListener(mTextPageListener!!)
            }

            // 设置视口
            mTextPageController!!.setViewPort(width, height)

            // 如果 model 中存在段落
            if (mTextModel!!.getChapterCount() > 0) {
                mTextPageController!!.skipPage(
                    mTextModel!!.getChapterCursor(0).getParagraphCursor(0)
                )
            }
        } else {
            mTextPageController!!.setViewPort(width, height)
        }
    }

    fun setPageListener(pageListener: TextPageListener) {
        mTextPageController?.setTextPageListener(pageListener)
        mTextPageListener = pageListener
    }

    fun getPageWidth(): Int {
        return mTextPageController?.pageWidth ?: 0
    }

    fun getPageHeight(): Int {
        return mTextPageController?.pageHeight ?: 0
    }

    /**
     * 是否页面存在
     */
    @Synchronized
    fun hasPage(type: PageType): Boolean {
        return mTextPageController?.hasPage(type) ?: false
    }

    /**
     * 当前章节的页面
     */
    fun hasPage(index: Int): Boolean {
        if (index < 0) {
            return false
        }
        return mTextPageController != null && (index < mTextPageController!!.getCurrentPageCount())
    }

    fun hasChapter(index: Int): Boolean {
        if (index < 0) {
            return false
        }
        return mTextModel != null && (index < mTextModel!!.getChapterCount())
    }

    fun hasChapter(type: PageType): Boolean {
        val curChapterCursor = getCurPageStartCursor()
            ?.getParagraphCursor()
            ?.getChapterCursor()
        return curChapterCursor?.hasChapter(type) ?: false
    }

    fun getPageCount(type: PageType): Int {
        return mTextPageController?.getPageCount(type) ?: 0
    }

    fun getPagePosition(type: PageType): PagePosition? {
        return mTextPageController?.getPagePosition(type)
    }

    fun getPageProgress(type: PageType): PageProgress? {
        return mTextPageController?.getPageProgress(type)
    }

    /**
     * 跳转页面
     */
    @Synchronized
    fun skipPage(position: TextPosition) {
        mTextPageController?.skipPage(position)
    }

    @Synchronized
    fun skipPage(position: PagePosition) {
        mTextPageController?.skipPage(position)
    }

    /**
     * 通知页面切换
     */
    @Synchronized
    fun turnPage(pageType: PageType) {
        mTextPageController?.turnPage(pageType)
    }

    /**
     * 获取当前页面的起始光标
     */
    @Synchronized
    fun getCurPageStartCursor(): TextWordCursor? {
        // 检测当前页面是否存在
        return if (!hasPage(PageType.CURRENT)) {
            null
        } else {
            mTextPageController?.getCurrentPage()?.startWordCursor
        }
    }

    /**
     * 获取当前页面的结尾光标
     */
    @Synchronized
    fun getCurPageEndCursor(): TextWordCursor? {
        // 检测当前页面是否存在
        return if (!hasPage(PageType.CURRENT)) {
            null
        } else {
            mTextPageController?.getCurrentPage()?.startWordCursor
        }
    }

    /**
     * 刷新 Engine，清空缓存
     */
    fun invalidate() {
        // TODO:待实现
    }

    // TODO:通知章节重新计算的方法
    fun completeInvalidte() {

    }

    /**
     * 是否支持断句
     */
    private fun isHyphenationPossible(): Boolean {
        return getTextStyle().allowHyphenations()
    }

    private var mCachedWord: TextWordElement? = null
    private var mCachedInfo: TextHyphenInfo? = null

    /**
     * 获取断句信息
     */
    private fun getHyphenationInfo(word: TextWordElement): TextHyphenInfo {
        // 如果缓存的 word 无效
        if (mCachedWord !== word) {
            mCachedWord = word
            mCachedInfo = TextTeXHyphenator.getInstance().getHyphenInfo(word)
        }
        return mCachedInfo!!
    }

    override fun onSizeChanged(width: Int, height: Int) {

        // TODO:重新渲染操作
        initTextPageController(width, height)
    }

    /**
     * 绘制页面，drawPage 只能绘制当前页的前一页和后一页。所以继续绘制下一页需要先进行
     * @see turnPage 翻页操作
     * @param canvas:被绘制的画布
     * @param pageType:绘制的页面类型
     */
    @Synchronized
    override fun drawInternal(canvas: TextCanvas, pageType: PageType) {
        // 检测资源变量是否存在，不存在则需要处理
        if (mTextModel == null || mTextModel!!.getChapterCount() == 0
            || mTextPageController == null
        ) {
            return
        }

        // 根据类型获取页面，如果页面不存在，则直接返回
        val page: TextPage = when (pageType) {
            PageType.PREVIOUS -> {
                mTextPageController!!.prevPage()
            }

            PageType.CURRENT -> {
                mTextPageController!!.getCurrentPage()
            }

            PageType.NEXT -> {
                mTextPageController!!.nextPage()
            }
        } ?: return

        // 准备 page 信息
        preparePage(page)

        // 准本文本绘制区域，并返回每个段落对应 textArea 的起始位置
        val labels = prepareTextArea(page)

        LogHelper.i(TAG, "prepareTextArea")

        // 绘制页面
        drawPage(canvas, page, labels)

        LogHelper.i(TAG, "drawPage")

        // 绘制高亮区域

        // 绘制下划线区域

        // 绘制之前已选中区域
    }

    /**
     * 查找页面的结尾光标
     */
    private fun findPageEndCursor(
        width: Int,
        height: Int,
        startWordCursor: TextWordCursor
    ): TextWordCursor {
        // 索引光标(生成快照，防止篡改 startWordCursor 数据)
        val findWordCursor = TextWordCursor(startWordCursor)
        // 当前行信息
        var curLine: TextLine? = null
        // 是否存在下一段
        var hasNextParagraph: Boolean
        // 剩余可用高度
        var remainAreaHeight = height

        do {
            // 重置文本样式
            resetTextStyle()
            // 上一段行信息
            val preLineInfo = curLine

            // 获取当前光标指向的位置
            val paragraphCursor = findWordCursor.getParagraphCursor()
            val curElementIndex = findWordCursor.getElementIndex()
            val curCharIndex = findWordCursor.getCharIndex()
            // 获取最后一个元素的索引
            val endElementIndex = paragraphCursor.getElementCount()

            // 查找当前位置之前的样式
            applyStyleChange(paragraphCursor, 0, curElementIndex)

            // 创建新的行信息
            curLine =
                TextLine(paragraphCursor, curElementIndex, curCharIndex, getTextStyle())

            // 循环遍历 element
            while (curLine!!.endElementIndex < endElementIndex) {
                // 根据 cursor 获取行数据
                curLine = prepareTextLine(
                    width,
                    paragraphCursor,
                    curLine.endElementIndex,
                    curLine.endCharIndex,
                    endElementIndex, preLineInfo
                )

                // 剩余高度 = 当前行高度 - 行高
                remainAreaHeight -= (curLine.height + curLine.descent)

                // 如果剩余高度不足，则 break
                if (remainAreaHeight <= 0) {
                    break
                }

                remainAreaHeight -= curLine.vSpaceAfter

                // 光标指向新行的末尾
                findWordCursor.moveTo(curLine.endElementIndex, curLine.endCharIndex)
            }

            // 是否存在下一段落,并移动到下一个段落
            hasNextParagraph =
                findWordCursor.isEndOfParagraph() && findWordCursor.moveToNextParagraph()

            // 是否存在剩余空间，是否存在下一段落，下一段落非结束类型段落
        } while (remainAreaHeight > 0 && hasNextParagraph && !findWordCursor.getParagraphCursor()
                .isEndOfSection()
        )

        // 重置文本样式
        resetTextStyle()

        return findWordCursor
    }

    /**
     * 根据页面的起始和结束光标，返回页面段落雷彪
     * @param page：准备的页面
     */
    private fun preparePage(page: TextPage) {
        // 如果 Page 已经准备，则不需要处理
        if (page.isPrepare) {
            return
        }

        val pageStartCursor = page.startWordCursor
        val pageEndCursor = page.endWordCursor

        // 查找光标
        val findCursor = TextWordCursor(pageStartCursor)
        // 文本行列表
        val textLines = page.textLineList
        // 当前行
        var curLine: TextLine? = null

        // 如果光标移动的位置小于界面结束位置
        while (findCursor.compareToIgnoreChar(pageEndCursor) < 0) {
            // 重置文本样式
            resetTextStyle()

            // 上一段行信息
            val preLineInfo = curLine
            // 获取当前光标指向的位置
            val paragraphCursor = findCursor.getParagraphCursor()
            val curElementIndex = findCursor.getElementIndex()
            val curCharIndex = findCursor.getCharIndex()

            // 获取最后一个元素的索引
            // 如果段落与结束索引同行，其最后一个元素索引，应该由 endCursor 决定
            val endElementIndex = if (paragraphCursor == pageEndCursor.getParagraphCursor()) {
                pageEndCursor.getElementIndex()
            } else {
                paragraphCursor.getElementCount()
            }

            // 查找当前位置之前的样式
            applyStyleChange(paragraphCursor, 0, curElementIndex)

            // 创建新的行信息
            curLine =
                TextLine(paragraphCursor, curElementIndex, curCharIndex, getTextStyle())

            // 遍历段落元素，生成行信息
            while (curLine!!.endElementIndex < endElementIndex) {
                // 填充 textLine
                curLine = prepareTextLine(
                    getPageWidth(),
                    paragraphCursor,
                    curLine.endElementIndex,
                    curLine.endCharIndex,
                    endElementIndex, preLineInfo
                )

                // 光标指向新行的末尾
                findCursor.moveTo(curLine.endElementIndex, curLine.endCharIndex)

                // 将获取的行信息存储
                textLines.add(curLine)
            }

            // 光标移动到下一段落
            findCursor.moveToNextParagraph()
        }

        // 重置文本样式
        resetTextStyle()

        // 设置页面准备成功
        page.isPrepare = true
    }

    /**
     * 准备行数据信息
     * @param lineWidth：一行的宽度
     * @param paragraphCursor：被分析的段落
     * @param startElementIndex：段落的起始元素
     * @param startCharIndex：段落的起始字节
     * @param endElementIndex：段落的结束元素
     * @param preLine：上一行信息
     */
    private fun prepareTextLine(
        lineWidth: Int,
        paragraphCursor: TextParagraphCursor,
        startElementIndex: Int,
        startCharIndex: Int,
        endElementIndex: Int,
        preLine: TextLine?
    ): TextLine {
        // 创建一个 TextLine
        val curLineInfo =
            TextLine(paragraphCursor, startElementIndex, startCharIndex, getTextStyle())

        // 索引标记
        var curElementIndex = startElementIndex
        var curCharIndex = startCharIndex

        // 是否是段落中的第一行
        val isFirstLine = startElementIndex == 0 && startCharIndex == 0

        if (isFirstLine) {
            // 获取当前元素
            var element = paragraphCursor.getElement(curElementIndex)

            if (element != null) {
                // 判断是否是文本样式元素
                while (isStyleElement(element!!)) {
                    // 使用该元素
                    applyStyleElement(element)
                    // 行起始位置向后移动一位
                    ++curElementIndex
                    curCharIndex = 0

                    // 如果起始位置指向到末尾位置
                    if (curElementIndex >= endElementIndex) {
                        break
                    }

                    // 获取下一个元素
                    element = paragraphCursor.getElement(curElementIndex)!!
                }
            }

            // 设置当前行的 Style
            curLineInfo.startStyle = getTextStyle()
            // 设置不具有改变 Style Element 的起始索引位置
            curLineInfo.realStartElementIndex = curElementIndex
            curLineInfo.realStartCharIndex = curCharIndex
        }

        // 获取当前样式
        var curTextStyle = getTextStyle()
        // 获取可绘制宽度 = 页面的宽度 - 右缩进
        val maxWidth = lineWidth - curTextStyle.getRightIndent(getMetrics())
        // 获取默认的缩进距离
        curLineInfo.leftIndent = curTextStyle.getLeftIndent(getMetrics())

        // 如果是第一行，且不为居中显示，则计算第一行的左缩进
        if (isFirstLine && curTextStyle.getAlignment() != TextAlignmentType.ALIGN_CENTER) {
            curLineInfo.leftIndent += curTextStyle.getFirstLineIndent(getMetrics())
        }

        // 如果 LeftIndent 太大了，则缩小
        if (curLineInfo.leftIndent > maxWidth - 20) {
            curLineInfo.leftIndent = maxWidth * 3 / 4
        }

        // 当前行的宽度，暂时为左缩进的宽度
        curLineInfo.width = curLineInfo.leftIndent

        // 如果实际起始位置为终止位置
        if (curLineInfo.realStartCharIndex == endElementIndex) {
            // 重置 end 信息
            curLineInfo.endElementIndex = curLineInfo.realStartElementIndex
            curLineInfo.endCharIndex = curLineInfo.realStartCharIndex
            return curLineInfo
        }

        var newWidth = curLineInfo.width
        var newHeight = curLineInfo.height
        var newDescent = curLineInfo.descent

        // 是否碰到了单词
        var wordOccurred = false
        var isVisible = false
        var lastSpaceWidth = 0
        // 空格数统计
        var internalSpaceCount = 0
        var removeLastSpace = false

        // 遍历 element 填充 TextLine
        while (curElementIndex < endElementIndex) {
            // 获取当前元素
            var element = paragraphCursor.getElement(curElementIndex)!!
            // 获取 Element 的宽度
            newWidth += getElementWidth(element, curCharIndex)
            // 获取 Element 最大高度
            newHeight = newHeight.coerceAtLeast(getElementHeight(element))
            // 文字距离基准线的最大距离
            newDescent = newDescent.coerceAtLeast(getElementDescent(element))

            // 根据 element 类型决定相应的处理方式
            if (element === TextElement.HSpace) {
                // 如果碰到了单词
                if (wordOccurred) {
                    wordOccurred = false
                    internalSpaceCount++
                    lastSpaceWidth = mPaintContext.getSpaceWidth()
                    newWidth += lastSpaceWidth
                }
            } else if (element === TextElement.NBSpace) {
                wordOccurred = true
            } else if (element is TextWordElement) {
                wordOccurred = true
                isVisible = true
            } else if (element is TextImageElement) {
                wordOccurred = true
                isVisible = true
            } else if (isStyleElement(element)) {
                applyStyleElement(element)
            }

            // 如果是元素偏移或者是文字元素造成当前测量宽度大于最大宽度的情况
            if (newWidth > maxWidth && (curLineInfo.endElementIndex != startElementIndex || element is TextWordElement)) {
                break
            }

            // 获取下一个元素。(用于解决换行的问题)

            ++curElementIndex
            curCharIndex = 0

            val previousElement = element

            // 判断是否到达了元素的结尾后一位 (处理完最后一个元素，判断换行)
            var allowBreak = curElementIndex >= endElementIndex

            // 不允许换行的情况
            if (!allowBreak) {
                // 新的 element
                element = paragraphCursor.getElement(curElementIndex)!!

                // 如果存在下列情况，进行强制换行
                allowBreak = previousElement !== TextElement.NBSpace &&
                        element !== TextElement.NBSpace &&
                        (element !is TextWordElement || previousElement is TextWordElement) &&
                        element !is TextImageElement &&
                        element !is TextControlElement
            }

            // 允许换行的情况，将计算的结果赋值给 TextLineInfo
            if (allowBreak) {
                curLineInfo.isVisible = isVisible
                curLineInfo.width = newWidth

                if (curLineInfo.height < newHeight) {
                    curLineInfo.height = newHeight
                }

                if (curLineInfo.descent < newDescent) {
                    curLineInfo.descent = newDescent
                }

                curLineInfo.endElementIndex = curElementIndex
                curLineInfo.endCharIndex = curCharIndex
                curLineInfo.spaceCount = internalSpaceCount

                curTextStyle = getTextStyle()
                removeLastSpace = !wordOccurred && internalSpaceCount > 0
            }
        }

        // 如果当前元素位置没有到达段落的末尾，并且允许断字
        if (curElementIndex < endElementIndex &&
            (isHyphenationPossible() || curLineInfo.endElementIndex == startElementIndex)
        ) {
            // 获取当前元素
            val element = paragraphCursor.getElement(curElementIndex)!!
            if (element is TextWordElement) {
                // 宽需要减去当前 element 的宽
                newWidth -= getWordWidth(element, curCharIndex)
                // 获取最大宽度和当前宽的差值，等于空格的宽度
                val remainSpaceWidth = maxWidth - newWidth
                // 如果当前元素大于 3 字节，并且剩余控件大于 2 倍的空格
                // 或者单个元素独占一行
                if ((element.length > 3 && remainSpaceWidth > 2 * mPaintContext.getSpaceWidth())
                    || curLineInfo.endElementIndex == startElementIndex
                ) {
                    // 获取断句信息
                    val hyphenInfo = getHyphenationInfo(element)

                    var hyphenIndex = curCharIndex
                    var subWordWidth = 0

                    var wordCurCharIndex = curCharIndex
                    var wordEndCharIndex = element.length - 1

                    // 如果末尾字节大于当前字节
                    while (wordEndCharIndex > wordCurCharIndex) {
                        // 取 word 的中间单词
                        val mid = (wordEndCharIndex + wordCurCharIndex + 1) / 2
                        var tempMid = mid
                        // 从 wordCurCharIndex 到 tempMid 之间查找支持 Hyphenation 的单词
                        while (tempMid > wordCurCharIndex && !hyphenInfo.isHyphenationPossible(
                                tempMid
                            )
                        ) {
                            --tempMid
                        }

                        // 如果查找到了
                        if (tempMid > wordCurCharIndex) {
                            // 获取单词的宽
                            val wordWidth = getWordWidth(
                                element,
                                curCharIndex,
                                tempMid - curCharIndex,
                                element.data[element.offset + tempMid - 1] != '-'
                            )
                            // 如果单词的宽小于剩余空间
                            if (wordWidth < remainSpaceWidth) {
                                wordCurCharIndex = mid
                                hyphenIndex = tempMid
                                subWordWidth = wordWidth
                            } else {
                                wordEndCharIndex = mid - 1
                            }
                        } else {
                            wordCurCharIndex = mid
                        }
                    }

                    if (hyphenIndex == curCharIndex && curLineInfo.endElementIndex == startElementIndex) {
                        subWordWidth = getWordWidth(element, curCharIndex, 1, false)
                        var right =
                            if (element.length == curCharIndex + 1) element.length else element.length - 1
                        var left = curCharIndex + 1
                        while (right > left) {
                            val mid = (right + left + 1) / 2
                            val w = getWordWidth(
                                element,
                                curCharIndex,
                                mid - curCharIndex,
                                element.data[element.offset + mid - 1] != '-'
                            )
                            if (w <= remainSpaceWidth) {
                                left = mid
                                subWordWidth = w
                            } else {
                                right = mid - 1
                            }
                        }
                        hyphenIndex = right
                    }

                    // 重置 TextLine 的样式
                    if (hyphenIndex > curCharIndex) {
                        curLineInfo.isVisible = true
                        curLineInfo.width = newWidth + subWordWidth
                        if (curLineInfo.height < newHeight) {
                            curLineInfo.height = newHeight
                        }
                        if (curLineInfo.descent < newDescent) {
                            curLineInfo.descent = newDescent
                        }
                        curLineInfo.endElementIndex = curElementIndex
                        curLineInfo.endCharIndex = hyphenIndex
                        curLineInfo.spaceCount = internalSpaceCount

                        curTextStyle = getTextStyle()
                        removeLastSpace = false
                    }
                }
            }
        }

        // 是否移除最后一个空格
        if (removeLastSpace) {
            curLineInfo.width -= lastSpaceWidth
            curLineInfo.spaceCount--
        }

        setTextStyle(curTextStyle)

        // 是否是第一行
        if (isFirstLine) {
            curLineInfo.vSpaceBefore = curLineInfo.startStyle.getSpaceBefore(getMetrics())
            if (preLine != null) {
                curLineInfo.previousInfoUsed = true
                curLineInfo.height += 0.coerceAtLeast(curLineInfo.vSpaceBefore - preLine.vSpaceAfter)
            } else {
                curLineInfo.previousInfoUsed = false
                curLineInfo.height += curLineInfo.vSpaceBefore
            }
        }

        // 如果是段落的最后一行
        if (curLineInfo.isEndOfParagraph()) {
            curLineInfo.vSpaceAfter = getTextStyle().getSpaceAfter(getMetrics())
        }

        // 如果遍历有问题，那直接到末尾
        if (curLineInfo.endElementIndex == startCharIndex && curLineInfo.endElementIndex == startCharIndex) {
            curLineInfo.endElementIndex = paragraphCursor.getElementCount()
            curLineInfo.endCharIndex = 0
        }
        return curLineInfo
    }

    /**
     * 根据 TextLine 设置文本绘制区域
     * @return 返回每行对应的 textElementAreaVector 的起始位置
     */
    private fun prepareTextArea(page: TextPage): IntArray {
        // 清空元素绘制区域数据
        page.textElementAreaVector.clear()

        // TODO: x,y 用于设置外边距的，暂未实现外边距的设置
        var x = 0
        var y = 0
        var previous: TextLine? = null
        // 记录每个位置对对应的 TextArea
        val labels = IntArray(page.textLineList.size + 1)

        // 页面的行信息
        page.textLineList.forEachIndexed { index, lineInfo ->
            lineInfo.adjust(previous)
            // 根据 curLineInfo 信息准备绘制区域
            prepareTextAreaInternal(page, lineInfo, x, y)
            // 记录当前高度
            y += lineInfo.height + lineInfo.descent + lineInfo.vSpaceAfter
            // 标记下一个文本行，对应的 area 列表的起始索引
            labels[index + 1] = page.textElementAreaVector.size()
            // 设置当前行变为上一行
            previous = lineInfo
        }
        return labels
    }

    /**
     * 根据文本行准备文本绘制区域信息
     */
    private fun prepareTextAreaInternal(
        page: TextPage,
        line: TextLine,
        x: Int,
        y: Int
    ) {
        var realX = x
        var realY = y
        // 根据视口的最大高度取最小值
        realY =
            (realY + line.height).coerceAtMost(getTextConfig().getMarginTop() + getPageHeight() - 1)

        val context = mPaintContext
        val paragraphCursor = line.paragraphCursor
        // 设置当前行的样式
        setTextStyle(line.startStyle)
        var spaceCount = line.spaceCount
        var fullCorrection = 0
        val isEndOfParagraph = line.isEndOfParagraph()
        // 是否碰到 word
        var isWordOccurred = false
        // 是否样式改变
        var isStyleChange = true

        realX += line.leftIndent

        val maxWidth = getPageWidth()

        when (getTextStyle().getAlignment()) {
            TextAlignmentType.ALIGN_RIGHT -> realX += maxWidth - getTextStyle().getRightIndent(
                getMetrics()
            ) - line.width
            TextAlignmentType.ALIGN_CENTER -> realX += (maxWidth - getTextStyle().getRightIndent(
                getMetrics()
            ) - line.width) / 2
            TextAlignmentType.ALIGN_JUSTIFY -> if (!isEndOfParagraph && paragraphCursor.getElement(
                    line.endElementIndex
                ) !== TextElement.AfterParagraph
            ) {
                fullCorrection =
                    maxWidth - getTextStyle().getRightIndent(getMetrics()) - line.width
            }
            TextAlignmentType.ALIGN_LEFT, TextAlignmentType.ALIGN_UNDEFINED -> {
            }
        }

        val paragraph = line.paragraphCursor
        val chapterIndex = paragraph.getChapterIndex()
        val paragraphIndex = paragraph.getParagraphIndex()
        val endElementIndex = line.endElementIndex
        var charIndex = line.realStartCharIndex
        var spaceElement: TextElementArea? = null
        run {
            var wordIndex = line.realStartElementIndex
            while (wordIndex < endElementIndex) {
                // 获取 Element
                val element = paragraph.getElement(wordIndex)
                val width = getElementWidth(element!!, charIndex)

                // 如果是空格元素
                if (element === TextElement.HSpace) {
                    if (isWordOccurred && spaceCount > 0) {
                        val correction = fullCorrection / spaceCount
                        val spaceLength = context.getSpaceWidth() + correction
                        // 是否是下划线
                        spaceElement = if (getTextStyle().isUnderline()) {
                            TextElementArea(
                                chapterIndex,
                                paragraphIndex,
                                wordIndex,
                                0,
                                0, // length
                                isLastElement = true, // is last in element
                                addHyphenationSign = false, // add hyphenation sign
                                isStyleChange = false, // changed style
                                style = getTextStyle(),
                                element = element,
                                startX = realX,
                                startY = realX + spaceLength,
                                endX = realY,
                                endY = realY
                            )
                        } else {
                            null
                        }
                        realX += spaceLength
                        fullCorrection -= correction
                        isWordOccurred = false
                        --spaceCount
                    }
                } else if (element is TextWordElement || element is TextImageElement) {
                    val height = getElementHeight(element)
                    val descent = getElementDescent(element)
                    val length = if (element is TextWordElement) element.length else 0
                    if (spaceElement != null) {
                        page.textElementAreaVector.add(spaceElement!!)
                        spaceElement = null
                    }
                    page.textElementAreaVector.add(
                        TextElementArea(
                            chapterIndex,
                            paragraphIndex,
                            wordIndex,
                            charIndex,
                            length - charIndex,
                            isLastElement = true, // is last in element
                            addHyphenationSign = false, // add hyphenation sign
                            isStyleChange = isStyleChange,
                            style = getTextStyle(),
                            element = element,
                            startX = realX,
                            startY = realX + width - 1,
                            endX = realY - height + 1,
                            endY = realY + descent
                        )
                    )
                    isStyleChange = false
                    isWordOccurred = true
                } else if (isStyleElement(element)) {
                    applyStyleElement(element)
                    isStyleChange = true
                }
                realX += width
                ++wordIndex
                charIndex = 0
            }
        }

        if (!isEndOfParagraph) {
            val len = line.endCharIndex
            if (len > 0) {
                val wordIndex = line.endElementIndex
                val wordElement = paragraph.getElement(wordIndex) as TextWordElement
                val addHyphenationSign = wordElement.data[wordElement.offset + len - 1] != '-'
                val width = getWordWidth(wordElement, 0, len, addHyphenationSign)
                val height = getElementHeight(wordElement)
                val descent = context.getDescent()
                page.textElementAreaVector.add(
                    TextElementArea(
                        chapterIndex,
                        paragraphIndex,
                        wordIndex, 0, len,
                        false, // is last in element
                        addHyphenationSign,
                        isStyleChange,
                        getTextStyle(),
                        wordElement,
                        realX, realX + width - 1, realY - height + 1, realY + descent
                    )
                )
            }
        }
    }

    /**
     * 绘制页面
     * @param lineOfAreaIndexArr:每个 textLine 对应 TextElementAreaVector 的起始位置s
     */
    private fun drawPage(canvas: TextCanvas, page: TextPage, lineOfAreaIndexArr: IntArray) {
        // 循环遍历文本行
        page.textLineList.forEachIndexed { index, textLineInfo ->
            drawTextLine(
                canvas,
                page,
                textLineInfo,
                lineOfAreaIndexArr[index],
                lineOfAreaIndexArr[index + 1]
            )
        }
    }

    /**
     * 绘制文本行
     */
    private fun drawTextLine(
        canvas: TextCanvas,
        page: TextPage,
        line: TextLine,
        fromAreaIndex: Int,
        toAreaIndex: Int
    ) {
        val paragraph = line.paragraphCursor
        var areaIndex = fromAreaIndex
        val endElementIndex = line.endElementIndex
        var charIndex = line.realStartCharIndex
        val pageAreas = page.textElementAreaVector.areas()

        if (toAreaIndex > pageAreas.size) {
            return
        }
        // 循环元素
        var wordIndex = line.realStartElementIndex
        while (wordIndex < endElementIndex && areaIndex < toAreaIndex) {
            val element = paragraph.getElement(wordIndex)
            val area = pageAreas[areaIndex]

            if (element === area.element) {
                ++areaIndex
                // 如果当前样式存在改变
                if (area.isStyleChange) {
                    setTextStyle(area.style)
                }

                val areaX = area.startX
                val areaY = area.endY - getElementDescent(element) - getTextStyle()
                    .getVerticalAlign(getMetrics())


                when (element) {
                    is TextWordElement -> {
                        drawWord(
                            canvas,
                            areaX,
                            areaY,
                            element,
                            charIndex,
                            -1,
                            false,
                            getTextConfig().getTextColor()
                        )
                    }
                    is TextImageElement -> {
                        canvas.drawImage(
                            areaX, areaY,
                            element.image,
                            getTextAreaSize()/*,
                            getScalingType(imageElement),
                            getAdjustingModeForImages()*/
                        )
                    }
                    TextElement.HSpace, TextElement.NBSpace -> {
                        val cw = mPaintContext.getSpaceWidth()
                        var len = 0
                        while (len < area.endX - area.startX) {
                            canvas.drawString(areaX + len, areaY, SPACE, 0, 1)
                            len += cw
                        }
                    }

                }
            }
            ++wordIndex
            charIndex = 0
        }

        if (areaIndex < toAreaIndex) {
            val area = pageAreas[areaIndex]
            if (area.isStyleChange) {
                setTextStyle(area.style)
            }
            val start = if (line.startElementIndex == line.endElementIndex) {
                line.startCharIndex
            } else 0

            val len = line.endCharIndex - start
            val word = paragraph.getElement(line.endElementIndex) as TextWordElement

            drawWord(
                canvas,
                area.startX,
                area.endY - mPaintContext.getDescent() - getTextStyle().getVerticalAlign(
                    getMetrics()
                ),
                word,
                start,
                len,
                area.addHyphenationSign, getTextConfig().getTextColor()
            )
        }
    }
}