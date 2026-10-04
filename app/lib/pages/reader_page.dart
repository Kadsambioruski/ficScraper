import 'package:app/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';

import '../../models/chapter.dart';


class ReaderPage extends StatefulWidget {
  final String fictionId;
  final List<Chapter> chapters;
  final int currentIndex;

  const ReaderPage({
    required this.fictionId,
    required this.chapters,
    required this.currentIndex,
    super.key,
  });

  @override
  State<ReaderPage> createState() => _ReaderPageState();
}

class _ReaderPageState extends State<ReaderPage> {
  int currentIndex = 0;

  final ScrollController _scrollController = ScrollController();

  bool reachedChapEnd = false;

  Future<void> saveProgress() async {
    final apiService = ApiService();

    await apiService.updateReaderProgress(
      widget.fictionId, 
      widget.chapters[currentIndex].chapterNumber,
    );
  }

  @override
  void initState() {
    super.initState();
    currentIndex = widget.currentIndex;
    _scrollController.addListener(_onScroll);
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;

    if (_scrollController.position.maxScrollExtent <= 0) return;

    final position = _scrollController.position;
    final atEnd = position.pixels >= position.maxScrollExtent - 50;
    if (atEnd && !reachedChapEnd) {
      reachedChapEnd = true;
    
      final chapterNumber = widget.chapters[currentIndex].chapterNumber;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(
          "You have reached the end of the chapter!\n" 
          "Updating read chapter to: $chapterNumber",
          ))
      );
      saveProgress();
    }

  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final chapter = widget.chapters[currentIndex];
    final bool hasPrevious = currentIndex > 0;
    final bool hasNext = currentIndex < widget.chapters.length - 1;
    return Scaffold(
      appBar: AppBar(
        title: Text(chapter.title),
      ),
      body: SingleChildScrollView(
        controller: _scrollController,
        key: ValueKey(chapter.chapterNumber),
        padding: const EdgeInsets.all(20),
        child: HtmlWidget(chapter.content),
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            ElevatedButton(
            onPressed: hasPrevious ? () {
                    setState(() {
                      currentIndex--;
                      reachedChapEnd = false;
                    });
                    WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (_scrollController.hasClients) {
                      _scrollController.jumpTo(0);
                    }
                  });
                  }
                : null,
          child: const Text("Previous"),
          ),
          Text("${currentIndex + 1} / ${widget.chapters.length}"),
          ElevatedButton(
          onPressed: hasNext ? () {
                  setState(() {
                    currentIndex++;
                    reachedChapEnd = false;
                  });
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (_scrollController.hasClients) {
                      _scrollController.jumpTo(0);
                    }
                  });
                }
              : null,
            child: Text("Next"),
            ),
          ],
        ),
      ),
    );
  }
}