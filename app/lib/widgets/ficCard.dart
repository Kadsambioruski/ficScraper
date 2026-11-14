import 'package:app/models/fiction.dart';
import 'package:flutter/material.dart';

class FicCard extends StatelessWidget {
  final Fiction fiction;
  final VoidCallback onTap;
  final VoidCallback? onFinish;
  final VoidCallback onRemove;

  const FicCard({
    required this.fiction,
    required this.onTap,
    this.onFinish,
    required this.onRemove,

    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final unread = fiction.chapAmount - fiction.readerProgress;
    return LayoutBuilder(
      builder: ((context, constraints) {
        final fontSize = constraints.maxWidth * 0.06;      
      return InkWell(
        onTap: onTap,
        child: Container(
          width: 300,
          padding: const EdgeInsets.all(20),
          margin: const EdgeInsets.only(right: 10),
          color: Theme.of(context).cardColor,
          child: Stack(
            children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  maxLines: 2,
                  fiction.title, 
                  style: 
                    TextStyle(
                      fontSize: fontSize,
                      fontWeight: FontWeight.bold),
                ),
                Text(fiction.author,
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text(fiction.site, 
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text('${fiction.chapAmount} chapters', 
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text('${fiction.wordCount} words', 
                style: 
                    TextStyle(
                      fontSize: fontSize),
                ),
                Text('${unread} unread chapter(s)', style: 
                    TextStyle(
                      fontSize: fontSize,
                      fontWeight: FontWeight.w600)
                ),
              ],
            ),
            Positioned(
              top: 0,
              right: 0,
              child: PopupMenuButton<String>(
                onSelected: (value) {
                  if (value == "finish") {
                    onFinish?.call();
                  } 
                  if (value == "remove") {
                    onRemove();
                  }
                },
                itemBuilder: (context) => const [
                  PopupMenuItem(
                    value: "finish",
                    child: Text("Finish")
                    ),
                  PopupMenuItem(
                    value: "remove",
                    child: Text("Remove")
                    ),
                  ],
                )
              )
            ]
          )
        ),
      );
      }));
  }
}
