import 'package:app/models/chapter.dart';
import 'package:app/models/fiction.dart';
import 'package:app/pages/reader_page.dart';
import 'package:app/services/api_service.dart';
import 'package:flutter/material.dart';

class ChapterPage extends StatefulWidget {
  final Fiction fiction;

  const ChapterPage({
    required this.fiction,
    super.key
  }); 

  @override
  State<ChapterPage> createState() => _ChapterPageState();
}

class _ChapterPageState extends State<ChapterPage> {
  late Fiction fiction;
  late Future<List<Chapter>> chapters;
  bool order = false;
  @override
  void initState() {
    super.initState();
    fiction = widget.fiction;
  }

  Future<void> refreshFiction() async {
    final updatedFiction = 
      await ApiService().getFiction(widget.fiction.id);
    
    setState(() {
      fiction = updatedFiction;
    });
  }


  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) {
          Navigator.pop(context, fiction);
        }
      },
      child: Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            IconButton(
              onPressed: () {
                setState(() {
                  order = !order;
                });
              }, 
              icon: Icon(Icons.sort)
              ),
            const Text("Chapters"),
          ],
        ),
        
      ),
      body: FutureBuilder<List<Chapter>>(
        future: chapters = ApiService().getChapters(widget.fiction.id), 
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('Error: ${snapshot.error}'),
            );
          }

          final chapters = snapshot.data ?? [];

          if (chapters.isEmpty) {
            return const Center(
              child: Text('No chapters found.'),
            );
          }

          final displayedChapters = order ? chapters.reversed.toList() : chapters;

          return ListView.builder(
            itemCount: displayedChapters.length,
            itemBuilder: (context, index) {
              final chapter = displayedChapters[index];
              final isUnread = chapter.isUnread(fiction.readerProgress);  
              
              return ListTile(
                title: Text(chapter.title),
                subtitle: Text("Chapter ${chapter.chapterNumber}"),
                tileColor: isUnread ? Colors.blueGrey : null,
                onTap: () async {
                  final originalIndex = chapters.indexOf(chapter);

                  await Navigator.push(context, MaterialPageRoute(
                    builder: (context) => ReaderPage(
                      fictionId: widget.fiction.id,
                      chapters: chapters,
                      currentIndex: originalIndex,
                      )
                    )
                  );
                  await refreshFiction();
                },
              );
            },
          );
        },
      ),
    )); 
  }
}
