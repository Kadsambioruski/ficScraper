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
    saveProgress();
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
                    });
                    saveProgress();
                  }
                : null,
          child: const Text("Previous"),
          ),
          Text("${currentIndex + 1} / ${widget.chapters.length}"),
          ElevatedButton(
          onPressed: hasNext ? () {
                  setState(() {
                    currentIndex++;
                  });
                  saveProgress();
                }
              : null,
           child: const Text("Next"),
           ),
          ],
        ),
      ),
    );
  }
}