import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';

import '../models/chapter.dart';

class ReaderPage extends StatelessWidget {
  final List<Chapter> chapters;
  final int currentIndex;

  const ReaderPage({
    required this.chapters,
    required this.currentIndex,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final chapter = chapters[currentIndex];
    final bool hasPrevious = currentIndex > 0;
    final bool hasNext = currentIndex < chapters.length - 1;
    return Scaffold(
      appBar: AppBar(
        title: Text(chapter.title),
      ),
      body: SingleChildScrollView(
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
                Navigator.pushReplacement(
                context, 
                MaterialPageRoute(builder: (context) => ReaderPage(
                  chapters: chapters, 
                  currentIndex: currentIndex - 1,
                  )
                )
              );
            } 
            : null,
          child: const Text("Previous"),
          ),
          Text("${currentIndex + 1} / ${chapters.length}"),
          ElevatedButton(onPressed: hasNext ? () {
            Navigator.pushReplacement(
              context, 
              MaterialPageRoute(
                builder: (context) => ReaderPage(
                  chapters: chapters, 
                  currentIndex: currentIndex + 1
                  )
                )
              );
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